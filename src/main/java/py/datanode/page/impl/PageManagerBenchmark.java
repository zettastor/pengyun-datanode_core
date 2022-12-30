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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import py.archive.ArchiveOptions;
import py.archive.page.PageAddress;
import py.archive.page.PageAddressImpl;
import py.archive.segment.SegId;
import py.common.NamedThreadFactory;
import py.common.reservoir.SlidingTimeWindowReservoir;
import py.datanode.configuration.DataNodeConfiguration;
import py.datanode.page.Page;
import py.datanode.page.PageManager;
import py.datanode.page.TaskType;
import py.datanode.page.context.PageContextFactory;
import py.storage.Storage;
import py.storage.impl.DummyStorage;

public class PageManagerBenchmark {
  private static final long _1G = 1024 * 1024 * 1024;

  private final AtomicLong count = new AtomicLong(0);
  private long lastCount = 0;
  private long lastTime = 0;

  private DataNodeConfiguration cfg;
  private StorageIoDispatcher storageIoDispatcher;
  private PageManager<Page> pageManager;
  private Map<Storage, ExecutorService> storageCacheExecutorMap;
  private SegId segId = new SegId(1, 1);
  private ConcurrentHashMap<Storage, AtomicLong> mapStorageToOffsets = new ConcurrentHashMap<>();

  public PageManagerBenchmark() {
    this.cfg = new DataNodeConfiguration();
    ArchiveOptions.initContants(cfg.getPageSize(), cfg.getSegmentUnitSize(), 1);
  }

  private static double mean(Long[] values) {
    if (values.length == 0) {
      return 0;
    }

    double sum = 0;
    for (long value : values) {
      sum += value;
    }
    return sum / values.length;
  }

  public static void main(String[] args) throws IOException {
    initLogger();
    int pageManagerCount = 1;
    int storageCount = 5;
    int concurrency = 256;
    int runTimeSeconds = 20;

    if (args.length > 0) {
      pageManagerCount = Integer.parseInt(args[0]);
    }

    if (args.length > 1) {
      storageCount = Integer.parseInt(args[1]);
    }

    if (args.length > 2) {
      concurrency = Integer.parseInt(args[2]);
    }

    if (args.length > 3) {
      runTimeSeconds = Integer.parseInt(args[3]);
    }

    System.out.println(String
        .format("page_manager_count %d, storage_count %d, concurrency %d, run_time_seconds %d",
            pageManagerCount, storageCount, concurrency, runTimeSeconds));

    PageManagerBenchmark pageManagerBenchmark = new PageManagerBenchmark();
    pageManagerBenchmark.start(pageManagerCount, storageCount, concurrency, runTimeSeconds);
  }

  public static void initLogger() throws IOException {
    PatternLayout layout = new PatternLayout();
    String conversionPattern = "%-5p[%d][%t]%C(%L):%m%n";
    layout.setConversionPattern(conversionPattern);

    ConsoleAppender consoleAppender = new ConsoleAppender();
    consoleAppender.setLayout(layout);
    consoleAppender.setTarget("System.out");
    consoleAppender.setEncoding("UTF-8");
    consoleAppender.activateOptions();

    Logger rootLogger = Logger.getRootLogger();
    rootLogger.setLevel(Level.WARN);
   
    rootLogger.removeAllAppenders();
    rootLogger.addAppender(consoleAppender);
  }

  public void start(int pageManagerCount, int storageCount, int concurrency, int runTimeSeconds) {
    cfg.setPageSystemCount(pageManagerCount);

    this.storageCacheExecutorMap = new ConcurrentHashMap<>();
    this.storageIoDispatcher = new StorageIoDispatcher();

    WrappedPageManagerFactory pageManagerFactory = new WrappedPageManagerFactory(cfg,
        storageIoDispatcher, new PageManagerDispatcher() {
          @Override
          public <P extends PageManager> P select(List<P> candidates, PageAddress address) {
            return candidates
                .get((int) ((address.getPhysicalOffsetInArchive()
                    / ArchiveOptions.PAGE_PHYSICAL_SIZE)
                    % candidates.size()));
          }
        });
    PageManager<Page> memoryPageManager = pageManagerFactory.build(2 * _1G, "benchmark");

    test(memoryPageManager, storageCount, concurrency, runTimeSeconds);
  }

  private void test(PageManager<Page> pageManager, int storageCount, int concurrency,
      int runTimeSeconds) {
    AtomicLong endTime = new AtomicLong(Long.MAX_VALUE);

    AtomicBoolean stopSignal = new AtomicBoolean(false);

    List<Storage> storageList = new ArrayList<>();
    Map<Storage, Semaphore> storageSemaphoreMap = new ConcurrentHashMap<>();
    Map<Storage, ExecutorService> storageExecutorServiceMap = new ConcurrentHashMap<>();
    Map<Storage, Thread> storageTestThreadMap = new ConcurrentHashMap<>();

    for (int i = 0; i < storageCount; i++) {
      Storage storage = new DummyStorage("test" + i, 1024);
      storageList.add(storage);
      storageSemaphoreMap.put(storage, new Semaphore(concurrency));
      storageExecutorServiceMap.put(storage,
          Executors
              .newSingleThreadExecutor(new NamedThreadFactory(storage.identifier() + "-callback")));
    }

    SlidingTimeWindowReservoir reservoir = new SlidingTimeWindowReservoir(10, TimeUnit.SECONDS);

    for (Storage storage : storageList) {
      Thread thread = new Thread(() -> {
        PageContextFactory<Page> pageContextFactory = new PageContextFactory<>();
        Semaphore semaphore = storageSemaphoreMap.get(storage);
        ExecutorService callbackExecutor = storageExecutorServiceMap.get(storage);
        while (!stopSignal.get()) {
          try {
            semaphore.acquire();
          } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
          }
          long submitTime = System.currentTimeMillis();
          count.incrementAndGet();
          pageManager
              .checkout(pageContextFactory.generateAsyncCheckoutContext(generateAddress(storage),
                  TaskType.CHECK_OUT_FOR_EXTERNAL_CORRECTION,
                  pageContext -> callbackExecutor.execute(() -> {
                    long time = System.currentTimeMillis() - submitTime;
                    reservoir.update(time);
                    pageContext.getPage().setPageLoaded(true);
                    pageContext.getPage().setDirty(true);
                    pageManager.checkin(pageContext);
                    semaphore.release();
                  }), cfg.getDefaultPageRequestTimeoutMs()));
        }
      }, storage.identifier());
      storageTestThreadMap.put(storage, thread);
    }

    ScheduledExecutorService monitor = Executors
        .newScheduledThreadPool(1, new NamedThreadFactory("monitor"));
    monitor.scheduleAtFixedRate(() -> {
      long timePassed = System.currentTimeMillis() - lastTime;
      long countInc = count.get() - lastCount;

      StringBuilder sb = new StringBuilder();

      double mean = mean(reservoir.getSnapshot());

      sb.append("\n").append("real time IOPMS : ").append(countInc / timePassed);
      sb.append(", average checkout time: ").append(mean).append("\n");

      for (Semaphore semaphore : storageSemaphoreMap.values()) {
        sb.append(semaphore.availablePermits()).append("\t");
      }

      System.out.println(sb);

      lastTime = System.currentTimeMillis();
      lastCount = count.get();

      if (System.currentTimeMillis() > endTime.get()) {
        System.out.println("shutting down..........................");
        stopSignal.set(true);

        try {
          for (Storage storage : storageList) {
            storageTestThreadMap.get(storage).join();
          }

          pageManager.close();

          for (Storage storage : storageList) {
            storageExecutorServiceMap.get(storage).shutdown();
            storageCacheExecutorMap.get(storage).shutdown();
          }
        } catch (InterruptedException e) {
          e.printStackTrace();
        }

        monitor.shutdown();
      }

    }, 1, 1, TimeUnit.SECONDS);

    try {
      Thread.sleep(2000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    for (Thread thread : storageTestThreadMap.values()) {
      thread.start();
    }

    endTime.set(System.currentTimeMillis() + runTimeSeconds * 1000);

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      long averageIoPs = count.get() / runTimeSeconds;
      System.out.println("overall iops : " + averageIoPs);
    }));
  }

  private PageAddress generateAddress(Storage storage) {
    return new PageAddressImpl(segId, 0,
        mapStorageToOffsets.computeIfAbsent(storage, s -> new AtomicLong(0)).incrementAndGet()
            * ArchiveOptions.PAGE_PHYSICAL_SIZE, storage);
  }
}
