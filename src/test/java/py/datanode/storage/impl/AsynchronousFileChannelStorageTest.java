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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import py.storage.Storage;
import py.storage.StorageIoType;
import py.storage.impl.AsyncStorage;
import py.storage.impl.AsynchronousFileChannelStorageFactory;
import py.test.TestBase;

public class AsynchronousFileChannelStorageTest extends TestBase {
  private static final Logger logger = LoggerFactory
      .getLogger(AsynchronousFileChannelStorageTest.class);
  private Path path = Paths.get("/tmp", "store.txt");
  
  
  @Before
  public void initEnv() throws IOException {
    Files.deleteIfExists(path);
    try {
      if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
        if (!Files.exists(path.getParent())) {
          Files.createDirectories(path.getParent());
        }
        Files.createFile(path);
      }
    } catch (Exception e) {
      logger.error("create file caught an exception ", e);
    }

    byte[] data = new byte[16 * 1024 * 1024];
    FileUtils.writeByteArrayToFile(path.toFile(), data);

    AsynchronousFileChannelStorageFactory asyncStorageFactory = 
        AsynchronousFileChannelStorageFactory
        .getInstance().setStorageIoType(
            StorageIoType.SYNCAIO);
    int threadCount = 2;
    asyncStorageFactory.setMaxThreadpoolSizePerStorage(threadCount);
    asyncStorageFactory.setMaxThreadpoolSizePerSsd(threadCount);
  }

  @After
  public void after() throws IOException {
    Files.deleteIfExists(path);
  }

  @After
  public void afterClass() {
    AsynchronousFileChannelStorageFactory.getInstance().close();
  }

  @Test
  public void testMulitReadAndWrite() throws Exception {
    AsynchronousFileChannelStorageFactory asyncStorageFactory =
        AsynchronousFileChannelStorageFactory
        .getInstance();
    int threadCount = 2;
    asyncStorageFactory.setMaxThreadpoolSizePerStorage(threadCount);

    @SuppressWarnings("unchecked") final AsyncStorage asyncStorage = 
        (AsyncStorage) asyncStorageFactory
        .generate(path.toString());
    int writeThreadCount = threadCount * 100;
    final CountDownLatch mainLatch = new CountDownLatch(writeThreadCount);

    final AtomicInteger writeComplelionCount = new AtomicInteger(0);
    final CompletionHandler<Integer, Object> handler1 = new CompletionHandler<Integer, Object>() {
      @Override
      public void completed(Integer result, Object attachment) {
        writeComplelionCount.incrementAndGet();
        logger.info("write completed, index: {}", attachment);
        mainLatch.countDown();
      }

      @Override
      public void failed(Throwable exc, Object attachment) {
      }
    };

    final AtomicInteger writeCount = new AtomicInteger(0);
    final CountDownLatch writeLatch = new CountDownLatch(1);

    final Random random = new Random();
    for (int i = 0; i < writeThreadCount; i++) {
      Thread thread = new Thread() {
        @Override
        public void run() {
          try {
            writeLatch.await();
            Integer value = writeCount.incrementAndGet();
            asyncStorage
                .write(ByteBuffer.allocate(1024), random.nextInt(1024) * 512, value, handler1);
          } catch (Exception e) {
            logger.error("caught an exception", e);
            mainLatch.countDown();
          }
        }
      };
      thread.start();
    }

    writeLatch.countDown();
    mainLatch.await();

    Assert.isTrue(writeCount.get() == writeComplelionCount.get(),
        "writeCount: " + writeCount.get() + ", writeComplelionCount: " + writeComplelionCount
            .get());
    Assert.isTrue(writeCount.get() == writeThreadCount);

  }

  @Test
  public void testAsyncFileChannelSyncReadAndWrite() throws InterruptedException, Exception {
    AsynchronousFileChannelStorageFactory asyncStorageFactory = 
        AsynchronousFileChannelStorageFactory
        .getInstance();

    Storage asyncStorage = asyncStorageFactory.generate(path.toString());

    assertNotNull(asyncStorage);
    int writeBufLength = 20;
    ByteBuffer writeBuffer = ByteBuffer.allocateDirect(writeBufLength);
    for (int i = 0; i < 10; i++) {
      writeBuffer.putChar('A');
    }

    writeBuffer.clear();
    assertTrue(writeBuffer.limit() == writeBufLength);
    asyncStorage.write(0, writeBuffer);

    ByteBuffer readBuffer = ByteBuffer.allocateDirect(writeBufLength);
    asyncStorage.read(0, readBuffer);

    assertTrue(writeBuffer.limit() == readBuffer.limit());
    for (int i = 0; i < readBuffer.limit(); i++) {
      logger.info(
          "From outside read index:[" + i + "]" + ", Read singal result:[" + readBuffer.get(i)
              + "]\n");
      assertTrue(writeBuffer.get(i) == readBuffer.get(i));
    }
  }

  @Test
  public void testAsyncFileChannelSyncReadAndWrite2() throws InterruptedException, Exception {
    AsynchronousFileChannelStorageFactory asyncStorageFactory = 
        AsynchronousFileChannelStorageFactory
        .getInstance();
    Storage asyncStorage = asyncStorageFactory.generate(path.toString());

    assertNotNull(asyncStorage);
    int writeBufLength = 20;
    byte[] writeBuffer = new byte[writeBufLength];
    for (int i = 0; i < writeBufLength; i++) {
      writeBuffer[i] = 'A';
    }

    assertTrue(writeBuffer.length == writeBufLength);
    asyncStorage.write(0, writeBuffer, 0, 20);

    byte[] readBuffer = new byte[writeBufLength];
    asyncStorage.read(0, readBuffer, 0, 20);

    assertTrue(writeBuffer.length == readBuffer.length);
    for (int i = 0; i < readBuffer.length; i++) {
      logger.info(
          "From outside read index:[" + i + "]" + ", Read singal result:[" + readBuffer[i] + "]\n");
      assertTrue(writeBuffer[i] == readBuffer[i]);
    }
  }

  @Test
  public void iodepth() throws Exception {
    AsynchronousFileChannelStorageFactory asyncStorageFactory = 
        AsynchronousFileChannelStorageFactory
        .getInstance();
    asyncStorageFactory.setMaxThreadpoolSizePerStorage(16);
    Storage storage = asyncStorageFactory.generate(path.toString());
    assertNotNull(storage);
    @SuppressWarnings("unchecked")
    AsyncStorage asyncStorage = (AsyncStorage) storage;

    int ioCount = 1000;
    final int ioSize = 16 * 1024;

    final CountDownLatch writeLatch = new CountDownLatch(ioCount);
    CompletionHandler<Integer, Object> handler1 = new CompletionHandler<Integer, Object>() {
      @Override
      public void completed(Integer result, Object attachment) {
        logger.info("write completed");
        writeLatch.countDown();
        assertEquals(result.intValue(), ioSize);
      }

      @Override
      public void failed(Throwable exc, Object attachment) {
        writeLatch.countDown();
      }
    };

    long startTime = System.currentTimeMillis();
    for (int i = 0; i < ioCount; i++) {
      asyncStorage.write(ByteBuffer.wrap(new byte[ioSize]), i * ioSize, null, handler1);
      logger.info("submit index: {}", i);
    }

    logger.info("submit over, cost time: {}", System.currentTimeMillis() - startTime);
    writeLatch.await();
    logger.info("write over, cost time: {}", System.currentTimeMillis() - startTime);
  }

  @Test
  public void testAsyncFileChannelAsyncWriteAndRead() throws Exception {
    AsynchronousFileChannelStorageFactory asyncStorageFactory = 
        AsynchronousFileChannelStorageFactory
        .getInstance();
    Storage storage = asyncStorageFactory.generate(path.toString());
    assertNotNull(storage);

    assertTrue(storage instanceof AsyncStorage);
    @SuppressWarnings("unchecked")
    AsyncStorage asyncStorage = (AsyncStorage) storage;

    ByteBuffer byteBuffer1 = ByteBuffer.allocate(1024);
    for (int i = 0; i < byteBuffer1.capacity(); i++) {
      byteBuffer1.put((byte) '2');
    }

    final CountDownLatch writeLatch = new CountDownLatch(1);
    CompletionHandler<Integer, Object> handler1 = new CompletionHandler<Integer, Object>() {
      @Override
      public void completed(Integer result, Object attachment) {
        writeLatch.countDown();
        logger.info("write completed");
      }

      @Override
      public void failed(Throwable exc, Object attachment) {
        writeLatch.countDown();
      }
    };

    byteBuffer1.clear();
    asyncStorage.write(byteBuffer1, 0, byteBuffer1, handler1);
    writeLatch.await();

    final CountDownLatch readLatch = new CountDownLatch(1);
    CompletionHandler<Integer, Object> handler2 = new CompletionHandler<Integer, Object>() {
      @Override
      public void completed(Integer result, Object attachment) {
        readLatch.countDown();
        logger.info("read completed");
      }

      @Override
      public void failed(Throwable exc, Object attachment) {
        readLatch.countDown();
      }
    };

    ByteBuffer byteBuffer2 = ByteBuffer.allocate(1024);
    asyncStorage.read(byteBuffer2, 0, byteBuffer2, handler2);
    readLatch.await();

    for (int i = 0; i < byteBuffer2.capacity(); i++) {
      assertTrue(byteBuffer2.get(i) == byteBuffer1.get(i));
    }
  }

}
