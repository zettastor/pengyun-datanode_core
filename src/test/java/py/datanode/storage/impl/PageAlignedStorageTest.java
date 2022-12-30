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

package py.datanode.storage.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import py.exception.StorageException;
import py.test.TestBase;

public class PageAlignedStorageTest extends TestBase {
  @Before
  public void init() throws Exception {
    super.init();
  }

  @Test
  public void basicTest() throws Exception {
    String filePath = "/tmp/testing_for_storage.bin";

    int finalFileSize = 10 * 1024 * 1024;
    createFile(filePath, finalFileSize);

    File file = new File(filePath);
    if (!file.exists()) {
      logger.warn("file:" + file.getAbsolutePath() + " does not exist");
      fail();
    }

    RandomAccessFile raf = new RandomAccessFile(file, "r");
    PageAlignedStorage rt = new PageAlignedStorage(file.getAbsolutePath(), raf);

    assertEquals((finalFileSize / PageAlignedStorage.DEFAULT_SECTOR_SIZE)
        * PageAlignedStorage.DEFAULT_SECTOR_SIZE, rt.size());
    rt.close();
  }

  @Test
  public void testPerformance() throws Exception {
    String filePath = "/tmp/testing_for_storage.bin";
    final int pageSize = 128 * 1024;
    int finalFileSize = 1024 * pageSize;
    createFile(filePath, finalFileSize);

    File file = new File(filePath);
    if (!file.exists()) {
      logger.warn("file:" + file.getAbsolutePath() + " does not exist");
      fail();
    }

    RandomAccessFile raf = new RandomAccessFile(file, "rw");
    final PageAlignedStorage rt = new PageAlignedStorage(file.getAbsolutePath(), raf);

    int totalRunningTime = 1000000;
    int numThreads = 100;
    final int runningTime = totalRunningTime / numThreads;

    ExecutorService executor = new ThreadPoolExecutor(numThreads, numThreads, 1000,
        TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
    List<Future<Long>> results = new ArrayList<>();
    results.add(executor.submit(new IoWorker(runningTime, pageSize, rt)));

    long totalTime = 0;
    for (Future<Long> result : results) {
      totalTime += result.get();
    }

    logger.info("TotalTime is " + totalTime 
        + " and BW is " + ((long) totalRunningTime * (long) pageSize * 1000L) / (1024 * totalTime) 
        + "KB" + " average latency is " + totalTime * 1000L / totalRunningTime);
  }

  private void createFile(String filePath, int fileSize) throws IOException {
    int blockSize = 1024 * 1024;
    FileOutputStream out = new FileOutputStream(filePath);
    while (fileSize > 0) {
      blockSize = blockSize < fileSize ? blockSize : fileSize;
      byte[] dataToWrite = new byte[blockSize];
      out.write(dataToWrite);
      fileSize -= blockSize;
    }
    out.close();
  }

  private class IoWorker implements Callable<Long> {
    private int runningTime;
    private int pageSize;
    private PageAlignedStorage storage;

    public IoWorker(int runningTime, int pageSize, PageAlignedStorage storage) {
      this.runningTime = runningTime;
      this.pageSize = pageSize;
      this.storage = storage;
    }

    @Override
    public Long call() throws Exception {
      final byte[] page = new byte[pageSize];
      final Random random = new Random(System.currentTimeMillis());
      int index = 0;
      long startTime = System.currentTimeMillis();
      while (index++ < runningTime) {
        long offset = random.nextInt(1024);
        try {
          storage.write(offset * pageSize, ByteBuffer.wrap(page));
        } catch (StorageException e) {
          logger.warn("caught an exception", e);
        }
      }
      return System.currentTimeMillis() - startTime;
    }
  }
}
