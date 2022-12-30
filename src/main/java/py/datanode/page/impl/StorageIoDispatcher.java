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

package py.datanode.page.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.datanode.exception.GenericPageSystemException;
import py.datanode.page.AvailablePageLister;
import py.datanode.page.DirtyPagePoolFactory;
import py.datanode.page.Page;
import py.datanode.page.PageContext;
import py.datanode.page.PageIoListener;
import py.datanode.page.context.BogusPageContext;
import py.datanode.storage.scheduler.StorageIoWorkerHouseKeeper;
import py.function.Callback;
import py.storage.Storage;

public class StorageIoDispatcher implements AvailablePageLister {
  private static final  Logger logger = LoggerFactory.getLogger(StorageIoDispatcher.class);
  private static final  int DEFAULT_INTERVAL_TIME_MS = 1000;
  private final BlockingQueue<PageContext<Page>> queue;
  private final AtomicBoolean started = new AtomicBoolean(false);
  
  private PageIoListener listener;
  private long lastCheckTime;
  private Thread thread;
  
  private long workerTimeoutMs = 10 * 60 * 1000;

  private DirtyPagePoolFactory dirtyPagePoolFactory;
  private StorageIoWorkerHouseKeeper storageIoWorkerHouseKeeper;
  private AvailablePageLister availablePageLister;

  public StorageIoDispatcher() {
    this.lastCheckTime = System.currentTimeMillis();
    this.queue = new LinkedBlockingQueue<>();
  }

  public void start(PageIoListener listener, StorageIoWorkerHouseKeeper storageIoWorkerHouseKeeper,
      DirtyPagePoolFactory dirtyPagePoolFactory, AvailablePageLister availablePageLister) {
    synchronized (started) {
      if (started.compareAndSet(false, true)) {
        if (this.listener == null && listener == null) {
          throw new IllegalArgumentException("can't start without a listener");
        }
        if (this.storageIoWorkerHouseKeeper == null && storageIoWorkerHouseKeeper == null) {
          throw new IllegalArgumentException(
              "can't start without a storage io worker house keeper");
        }
        if (this.dirtyPagePoolFactory == null && dirtyPagePoolFactory == null) {
          throw new IllegalArgumentException("can't start without a dirty page pool factory");
        }
        if (this.availablePageLister == null && availablePageLister == null) {
          throw new IllegalArgumentException("can't start without a available page listener");
        }

        if (storageIoWorkerHouseKeeper != null) {
          this.storageIoWorkerHouseKeeper = storageIoWorkerHouseKeeper;
        }
        if (dirtyPagePoolFactory != null) {
          this.dirtyPagePoolFactory = dirtyPagePoolFactory;
        }
        if (availablePageLister != null) {
          this.availablePageLister = availablePageLister;
        }
        if (listener != null) {
          this.listener = listener;
        }

        this.thread = new ContextPuller();
      }
    }
  }

  private void process(PageContext<Page> context) throws Exception {
    try {
      
     
      switch (context.getIoType()) {
        case TODISK:
        case TODISKFOREXTERNAL:
          storageIoWorkerHouseKeeper.getOrBuildStorageIoWorker(context.getStorage())
              .submitWrite(new Callback() {
                @Override
                public void completed() {
                  listener.flushedToStorage(context);
                }

                @Override
                public void failed(Throwable e) {
                  context.setCause((Exception) e);
                  listener.flushedToStorage(context);
                }
              }, context.getPage(), context.getIoType().isExternal());
          break;
        case FROMDISK:
        case FROMDISKFOREXTERNAL:
          storageIoWorkerHouseKeeper.getOrBuildStorageIoWorker(context.getStorage())
              .submitRead(new Callback() {
                  @Override
                  public void completed() {
                    listener.loadedFromStorage(context);
                  }

                  @Override
                  public void failed(Throwable e) {
                    context.setCause((Exception) e);
                    listener.loadedFromStorage(context);
                  }
              }, context.getPageAddressForIo(), context.getPage(),
                  context.getIoType().isExternal());
          break;
        default:
          logger.error("the task is {}, type is {}", context, context.getIoType());
          throw new NotImplementedException("");
      }
    } catch (Exception e) {
      logger.error("caught an exception, context={}", context, e);
      context.setCause(new GenericPageSystemException(e));
      if (context.getIoType().isRead()) {
        listener.loadedFromStorage(context);
      } else {
        listener.flushedToStorage(context);
      }
    }

  }

  public void submit(PageContext<Page> context) {
    Validate.isTrue(queue.add(context));
  }

  public void setWorkerTimeout(long workerTimeoutMs) {
    this.workerTimeoutMs = workerTimeoutMs;
  }

  public void stop() {
    synchronized (started) {
      if (started.compareAndSet(true, false)) {
        try {
          if (thread != null) {
            submit(BogusPageContext.defaultBogusPageContext);
            thread.join();
          }
        } catch (Exception e) {
          logger.warn("caught an exception", e);
        }
        storageIoWorkerHouseKeeper.stopAllAndClear();
      }
    }
  }

  public StorageIoWorkerHouseKeeper getStorageIoWorkerHouseKeeper() {
    return storageIoWorkerHouseKeeper;
  }

  public DirtyPagePoolFactory getDirtyPagePoolFactory() {
    return dirtyPagePoolFactory;
  }

  @Override
  public void hasAvailblePage() {
    this.availablePageLister.hasAvailblePage();
  }

  @Override
  public void hasAvailblePage(Storage storage) {
    this.availablePageLister.hasAvailblePage(storage);
  }

  private class ContextPuller extends Thread {
    public ContextPuller() {
      super("page-io-dispatcher");
      start();
    }

    @Override
    public void run() {
      try {
        Collection<PageContext<Page>> contexts = new ArrayList<PageContext<Page>>();

        while (true) {
          contexts.clear();
          if (queue.drainTo(contexts) == 0) {
            PageContext<Page> context = queue.poll(DEFAULT_INTERVAL_TIME_MS, TimeUnit.MILLISECONDS);
            if (context != null) {
              contexts.add(context);
            }
          }

          long currentTime = System.currentTimeMillis() - workerTimeoutMs;
          if (currentTime > lastCheckTime) {
            storageIoWorkerHouseKeeper.removeUnusedWorkers(currentTime);
            lastCheckTime = System.currentTimeMillis();
          }

          if (contexts.size() == 0) {
            continue;
          }

          for (PageContext<Page> context : contexts) {
            if (context == BogusPageContext.defaultBogusPageContext) {
              logger.warn("exit the page io dispatcher thread");
              return;
            }

            process(context);
          }
        }
      } catch (Throwable t) {
        logger.error("page io dispatcher exit", t);
      }
    }
  }

}
