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

package py.datanode.utils;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.exception.StorageException;
import py.storage.Storage;
import py.storage.StorageFactory;
import py.storage.impl.AsynchronousFileChannelStorage;

public class DummyAsyncStorageFactory implements StorageFactory {
  private static final Logger logger = LoggerFactory.getLogger(DummyAsyncStorageFactory.class);
  private long size;
  private ExecutorService executor;
  private List<Storage> storages = new LinkedList<Storage>();

  public DummyAsyncStorageFactory setSize(long size) {
    this.size = size;
    return this;
  }

  public DummyAsyncStorageFactory setExecutorService(ExecutorService executor) {
    this.executor = executor;
    return this;
  }

  @Override
  public Storage generate(String id) throws StorageException {
    return generate(id, 1, 1);
  }

  public Storage generate(String id, int ioDepth, int threadSize) throws StorageException {
    File file = new File(id);
    if (file.exists()) {
      file.delete();
    }
    int sectorSize = 512;
    long remaining = size / sectorSize;
    RandomAccessFile randomAccessFile = null;
    try {
      randomAccessFile = new RandomAccessFile(file, "rw");

      logger.warn("allocate size {} for file {}", sectorSize, id);
      ByteBuffer zeros = ByteBuffer.allocate(sectorSize);
      while (remaining-- > 0) {
        randomAccessFile.write(zeros.array());
      }
    } catch (Exception e) {
      logger.warn("remaining{}, caught an exception", remaining, e);
    } finally {
      if (randomAccessFile != null) {
        try {
          randomAccessFile.close();
        } catch (Exception e) {
          logger.warn("caught an exception");
        }
      }
    }

    Storage storage = new AsynchronousFileChannelStorage(Paths.get(id), ioDepth, threadSize,
        executor);
    storages.add(storage);
    return storage;
  }

  public AsynchronousFileChannelStorage generateWithExistFile(String id) throws StorageException {
    AsynchronousFileChannelStorage storage = new AsynchronousFileChannelStorage(Paths.get(id), 16,
        4, executor);
    storages.add(storage);

    return storage;
  }

  public void delete() {
    for (Storage storage : storages) {
      try {
        storage.close();
        new File(storage.identifier()).delete();
      } catch (Exception e) {
        logger.warn("caught an exception", e);
      }
    }

    storages.clear();
  }
}
