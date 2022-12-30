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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import py.archive.page.PageAddress;
import py.archive.page.PageAddressImpl;
import py.archive.segment.SegId;
import py.common.Utils;
import py.datanode.configuration.DataNodeConfiguration;
import py.datanode.page.Page;
import py.datanode.test.DataNodeConfigurationForTest;
import py.function.Callback;
import py.storage.Storage;
import py.test.TestBase;

public class StorageIoWorkerImplTest extends TestBase {
  private final SegId segId = new SegId(1L, 0);
  private final DataNodeConfiguration cfg = new DataNodeConfigurationForTest();

  private final AtomicInteger finishedCount = new AtomicInteger(0);
  private final Semaphore semaphore = new Semaphore(0);

  @Test
  public void test() throws Exception {
    StorageIoWorkerImpl storageIoWorker = new StorageIoWorkerImplForTest("test", cfg);

    storageIoWorker.start();

    Storage storage = mock(Storage.class);

    int pageCount = 16384;

    List<Page> pages = generatePages(pageCount, storage);

    semaphore.release(1024);

    for (Page page : pages) {
      semaphore.acquire();
      storageIoWorker.add(new Callback() {
        @Override
        public void completed() {
          finishedCount.incrementAndGet();
          semaphore.release();
        }

        @Override
        public void failed(Throwable e) {
          logger.error("caught an exception", e);
        }
      }, page, false);
    }

    Utils.waitUntilConditionMatches(5, () -> finishedCount.get() == pageCount);
    storageIoWorker.stop();
  }

  @Test
  public void testReadOne() throws Exception {
    StorageIoWorkerImpl storageIoWorker = new StorageIoWorkerImplForTest("test", cfg);

    storageIoWorker.start();

    Storage storage = mock(Storage.class);

    int pageCount = 1;

    List<Page> pages = generatePages(pageCount, storage);

    semaphore.release(1024);

    for (Page page : pages) {
      semaphore.acquire();
      storageIoWorker.submitRead(new Callback() {
        @Override
        public void completed() {
          finishedCount.incrementAndGet();
          semaphore.release();
        }

        @Override
        public void failed(Throwable e) {
          logger.error("caught an exception", e);
        }
      }, page.getAddress(), page, false);
    }

    Utils.waitUntilConditionMatches(5, () -> finishedCount.get() == pageCount);
    storageIoWorker.stop();
  }

  @Test
  public void testWriteOne() throws Exception {
    StorageIoWorkerImpl storageIoWorker = new StorageIoWorkerImplForTest("test", cfg);

    storageIoWorker.start();

    Storage storage = mock(Storage.class);

    int pageCount = 1;

    List<Page> pages = generatePages(pageCount, storage);

    semaphore.release(1024);

    for (Page page : pages) {
      semaphore.acquire();
      storageIoWorker.add(new Callback() {
        @Override
        public void completed() {
          finishedCount.incrementAndGet();
          semaphore.release();
        }

        @Override
        public void failed(Throwable e) {
          logger.error("caught an exception", e);
        }
      }, page, false);
    }

    Utils.waitUntilConditionMatches(5, () -> finishedCount.get() == pageCount);
    storageIoWorker.stop();
  }

  private List<Page> generatePages(int count, Storage storage) {
    List<Page> pages = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      Page page = mock(Page.class);
      when(page.isDirty()).thenReturn(true);
      when(page.canFlush()).thenReturn(true);
      when(page.getAddress())
          .thenReturn(new PageAddressImpl(segId, i * cfg.getPageSize(), 0, storage));
      pages.add(page);
    }
    return pages;
  }

  public static class StorageIoWorkerImplForTest extends StorageIoWorkerImpl {
    ExecutorService executorService = Executors.newSingleThreadExecutor();

    StorageIoWorkerImplForTest(String prefix, DataNodeConfiguration configuration) {
      super(prefix, configuration);
    }

    @Override
    public void readFromHdd(PageAddress pageAddress, Page page, Callback callback) {
      executorService.execute(() -> {
        try {
          Thread.sleep(1);
        } catch (InterruptedException ignore) {
          logger.error("", ignore);
        }
        callback.completed();
      });
    }

    @Override
    public void writeIntoHdd(PageAddress pageAddress, Page page, Callback callback) {
      executorService.execute(() -> {
        try {
          Thread.sleep(1);
        } catch (InterruptedException ignore) {
          logger.error("", ignore);
        }
        callback.completed();
      });
    }
  }
}