/*
 * Copyright (c) 2022. PengYunNetWork
 *
 * This program is free software: you can use, redistribute, and/or modify it
 * under the terms of the GNU Affero General Public License, version 3 or later ("AGPL"),
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 *  You should have received a copy of the GNU Affero General Public License along with
 *  this program. If not, see <http://www.gnu.org/licenses/>.
 */

package py.datanode.storage.scheduler;

import static py.datanode.storage.context.StorageIoContext.StorageIoType.READ;
import static py.datanode.storage.context.StorageIoContext.StorageIoType.WRITE;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.archive.page.PageAddress;
import py.datanode.configuration.DataNodeConfiguration;
import py.datanode.page.DirtyPagePool;
import py.datanode.page.Page;
import py.datanode.page.impl.GarbagePageAddress;
import py.datanode.page.impl.PageIoHelper;
import py.datanode.storage.context.ReadPageIoContext;
import py.datanode.storage.context.StorageIoContext;
import py.datanode.storage.context.StorageIoContext.StorageIoType;
import py.datanode.storage.context.WritePageIoContext;
import py.function.Callback;
import py.io.sequential.IoSequentialTypeHolder.IoSequentialType;
import py.io.sequential.RandomSequentialIdentifierV2;
import py.io.sequential.RandomSequentialIdentifierV2Impl;

public class StorageIoWorkerImpl implements StorageIoWorker, DirtyPagePool {
  private static final Logger logger = LoggerFactory.getLogger(StorageIoWorkerImpl.class);

  private static final int LOWER_PENDING_TASK_COUNT_THRESHOLD = 128;

  private static final int UPPER_PENDING_TASK_COUNT_THRESHOLD = 256;

  private final String prefix;
  private final AtomicBoolean isStopped = new AtomicBoolean(true);
  private final Thread workingThread;

  private final StorageIoQueue<StorageIoContext> readQueue;

  private final StorageIoQueue<StorageIoContext> writeQueue;

  private final StorageIoQueue<WritePageIoContext> dirtyPagesQueue;

  private final AtomicBoolean signalPendingTask = new AtomicBoolean(false);

  private final PriorityQueue<StorageIoContext> workingQueue = new PriorityQueue<>(
      Comparator.comparingLong(StorageIoContext::getOffsetOnArchive));

  private final ReentrantLock lock = new ReentrantLock();
  private final Condition gotWorkToDo = lock.newCondition();

  private final DataNodeConfiguration configuration;

  private final RandomSequentialIdentifierV2 sequentialIdentifierV2;
  
  private long lastIoTime;

  StorageIoWorkerImpl(String prefix,
      DataNodeConfiguration configuration) {
    this.prefix = prefix;
    this.configuration = configuration;
    this.dirtyPagesQueue = new SwappingSortedStorageIoQueue<>(this::newTasksAvailable,
        LOWER_PENDING_TASK_COUNT_THRESHOLD, UPPER_PENDING_TASK_COUNT_THRESHOLD,
        WritePageIoContext::compareTo);

    this.readQueue = new SwappingSortedStorageIoQueue<>(this::newTasksAvailable,
        LOWER_PENDING_TASK_COUNT_THRESHOLD, UPPER_PENDING_TASK_COUNT_THRESHOLD,
        Comparator.comparingLong(StorageIoContext::getOffsetOnArchive));

    this.writeQueue = new SingleStorageIoQueue<>(this::newTasksAvailable,
        LOWER_PENDING_TASK_COUNT_THRESHOLD,
        UPPER_PENDING_TASK_COUNT_THRESHOLD);

    this.sequentialIdentifierV2 = new RandomSequentialIdentifierV2Impl(
        configuration.getCacheStorageIoSequentialCondition(), prefix);
    this.workingThread = new Thread(this::doJob, "storage-io-worker-" + prefix);
    lastIoTime = System.currentTimeMillis();
  }

  @Override
  public void start() {
    synchronized (isStopped) {
      if (isStopped.compareAndSet(true, false)) {
        workingThread.start();
      }
    }
  }

  @Override
  public void stop() {
    synchronized (isStopped) {
      if (isStopped.compareAndSet(false, true)) {
        logger.warn("going to stop the storage IO worker {}", prefix);

        newTasksAvailable();

        try {
          workingThread.join();
        } catch (InterruptedException e) {
          logger.warn("joining thread interrupted", e);
        }
      }
    }
  }

  @Override
  public void submitRead(Callback callback, PageAddress pageAddress, Page page, boolean external) {
    ReadPageIoContext readPageIoContext = new ReadPageIoContext(callback, pageAddress, page,
        external);
    readQueue.offer(readPageIoContext);
  }

  @Override
  public void submitWrite(Callback callback, Page page, boolean external) {
    WritePageIoContext writePageIoContext = new WritePageIoContext(callback, page, external);
    writeQueue.offer(writePageIoContext);
  }

  @Override
  public long lastIoTime() {
    return lastIoTime;
  }

  @Override
  public int pendingRequestCount() {
    return readQueue.queueLength() + writeQueue.queueLength() + dirtyPagesQueue.queueLength()
        + workingQueue.size();
  }

  @Override
  public void notifyPendingWork() {
    signalPendingTask.set(true);
    newTasksAvailable();
  }

  @Override
  public void add(Callback callback, Page page, boolean external) {
    WritePageIoContext writePageIoContext = new WritePageIoContext(callback, page, external);
    dirtyPagesQueue.offer(writePageIoContext);
  }

  private void newTasksAvailable() {
    lock.lock();
    try {
      gotWorkToDo.signal();
    } finally {
      lock.unlock();
    }
  }

  private int drainWorkingTask(PriorityQueue<StorageIoContext> queue,
      List<StorageIoContext> ioRwContexts, int max) {
    for (int i = 0; i < max; i++) {
      StorageIoContext ioContext = queue.poll();
      if (ioContext != null) {
        ioRwContexts.add(ioContext);
      } else {
        break;
      }
    }
    return ioRwContexts.size();
  }

  private void doRwIoContexts(List<StorageIoContext> ioRwContexts) {
    StorageIoContext processingContext;
    IoSequentialType lastValidIoSequentialType = null;
    StorageIoType lastValidIoType = null;
    sequentialIdentifierV2.judgeIoIsSequential(ioRwContexts);
    for (int i = 0; i < ioRwContexts.size(); i++) {
      processingContext = ioRwContexts.get(i);

      if (processingContext.getIoSequentialType() == IoSequentialType.UNKNOWN) {
        if (lastValidIoSequentialType != null && lastValidIoType != null) {
          processingContext.setIoSequentialType(lastValidIoSequentialType);
        } else {
          processingContext.setIoSequentialType(IoSequentialType.RANDOM_TYPE);
        }

      } else {
        lastValidIoSequentialType = processingContext.getIoSequentialType();
        lastValidIoType = processingContext.getIoType();
      }

      process(processingContext, false);
    }
    ioRwContexts.clear();
  }

  private void doJob() {
    StorageIoContext processingContext;
    while (true) {
      lock.lock();
      try {
        if (!hasWorkToDo()) {
          logger.debug("nothing to do, wait");
         
          gotWorkToDo.await(5, TimeUnit.SECONDS);
        }
      } catch (InterruptedException ignore) {
        logger.error("InterruptedException", ignore);
      } finally {
        lock.unlock();
      }

      drainTasksToWorkingQueue(128);

      logger.debug("got {} tasks to do", workingQueue.size());

      List<StorageIoContext> ioRwContexts = new ArrayList<>();
      while (drainWorkingTask(workingQueue, ioRwContexts,
          UPPER_PENDING_TASK_COUNT_THRESHOLD) > 0) {
        doRwIoContexts(ioRwContexts);
      }

      if (isStopped.get()) {
       

        logger.warn("got stop signal, try to finish all the rest tasks");

        drainTasksToWorkingQueue(Integer.MAX_VALUE);

        logger.warn("last round we've got {} tasks", workingQueue.size());

        while ((processingContext = workingQueue.poll()) != null) {
          process(processingContext, false);
        }

        logger.warn("all tasks finished, exit");

        drainTasksToWorkingQueue(Integer.MAX_VALUE);
        if (workingQueue.isEmpty()) {
          break;
        }
      }
    }
  }

  private void drainTasksToWorkingQueue(int maxVal) {
   
    readQueue.generateContexts(workingQueue, Integer.MAX_VALUE);

    writeQueue.generateContexts(workingQueue, maxVal);

    if (configuration.isStartFlusher()) {
      dirtyPagesQueue.generateContexts(workingQueue, maxVal);
    }
  }

  private boolean hasWorkToDo() {
    lock.lock();
    try {
      boolean hasWorkToDo =
          readQueue.hasAvailableTasks() || writeQueue.hasAvailableTasks() || dirtyPagesQueue
              .hasAvailableTasks();

      return hasWorkToDo;
    } finally {
      lock.unlock();
    }
  }

  private void process(StorageIoContext context, boolean retry) {
   
    logger.debug("processing {}", context);
    lastIoTime = System.currentTimeMillis();
    if (context.markProcessing() || retry) {
      StorageIoType ioType = context.getIoType();
      if (ioType == StorageIoType.WRITE) {
        processWrite((WritePageIoContext) context);
      } else if (ioType == StorageIoType.READ) {
        processRead((ReadPageIoContext) context);
      } else {
        logger.error("the context {} can not process", context);
      }

    } else {
      logger.debug("the context is no need {}", context);
      context.discard();
    }
  }

  private void processWrite(WritePageIoContext context) {
    writeIntoHdd(context.getPage().getAddress(), context.getPage(), context);
  }

  private void processRead(ReadPageIoContext context) {
    readFromHdd(context.getPageAddress(), context.getPage(), context);
  }

  private void costWarning(long waitTimeNanos, StorageIoContext context) {
    long waitTimeMillis = TimeUnit.NANOSECONDS.toMillis(waitTimeNanos);
    if (context.getIoType() == READ) {
      assert (context instanceof ReadPageIoContext);
      PageAddress pageAddress = ((ReadPageIoContext) context).getPage().getAddress();
      if (((ReadPageIoContext) context).isExternal()) {
        if (waitTimeMillis > configuration.getIoTaskThresholdMs()) {
          logger.warn("a read request waiting too long {}, {}", waitTimeMillis, pageAddress);
        }
      }
      long time = System.currentTimeMillis();
      context.addFinishHooker(() -> {
        long timeProcess = System.currentTimeMillis() - time;
        if (timeProcess > configuration.getIoTaskThresholdMs()) {
          logger.warn("a read request processing too long {}, {}", waitTimeMillis, pageAddress);
        }
      });
    } else if (context.getIoType() == WRITE) {
      assert (context instanceof WritePageIoContext);
      PageAddress pageAddress = ((WritePageIoContext) context).getPage().getAddress();
      if (((WritePageIoContext) context).isExternal()) {
        if (waitTimeMillis > configuration.getIoWriteTaskThresholdMs()) {
          logger.warn("a write request waiting too long {}, {}", waitTimeMillis, pageAddress);
        }
      }
      long time = System.currentTimeMillis();
      context.addFinishHooker(() -> {
        long timeProcess = System.currentTimeMillis() - time;
        if (timeProcess > configuration.getIoWriteTaskThresholdMs()) {
          logger.warn("a write request processing too long {}, {}", waitTimeMillis, pageAddress);
        }
      });
    }
  }

  public void readFromHdd(PageAddress pageAddress, Page page, Callback callback) {
    try {
      logger.debug("a page address {}", pageAddress);
      Validate.isTrue(!(pageAddress instanceof GarbagePageAddress));
      PageIoHelper
          .loadFromStorage(pageAddress, page, callback,
              configuration.isPageMetadataNeedFlushToDisk());
    } catch (Exception e) {
      logger.warn("HDD read pageAddress:{} exception.", pageAddress, e);
      callback.failed(e);
    }
  }

  public void writeIntoHdd(PageAddress pageAddress, Page page, Callback callback) {
    logger.debug("write into HDD page address:{}", pageAddress);
    Validate.isTrue(!(pageAddress instanceof GarbagePageAddress));
    try {
      if (configuration.isPageMetadataNeedFlushToDisk()) {
        PageIoHelper.flushToStorage(pageAddress, page.getIoBuffer(), callback);
      } else {
        PageIoHelper.flushToStorage(pageAddress, page.getDataBuffer(), callback);
      }
    } catch (Exception e) {
      logger.warn("HDD write pageAddress:{} exception.", pageAddress, e);
      callback.failed(e);
    }
  }
}
