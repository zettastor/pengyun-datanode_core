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

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.exception.StorageException;
import py.storage.Storage;
import py.storage.impl.AsyncStorage;
import py.storage.impl.DummyStorageFactory;
import py.storage.impl.FileStorageFactory;

public class StorageBuilder {
  private static final Logger logger = LoggerFactory.getLogger(StorageBuilder.class);

  public static List<Storage> getFileStorage(File storageDir, FileStorageFactory factory)
      throws StorageException {
    List<Storage> stores = new LinkedList<Storage>();

    if (!storageDir.exists()) {
      throw new StorageException("Directory does not exist; dir:" + storageDir);
    }

    if (!storageDir.isDirectory()) {
      throw new StorageException("Expected a directory; dir:" + storageDir);
    }

    File[] files = storageDir.listFiles();
    if (files == null || files.length == 0) {
      logger.warn("Directory is empty; dir:" + storageDir);
      return stores;
    }

    for (File file : files) {
      Storage storage = null;
     
      try {
        storage = factory.setFile(file).generate(file.getAbsolutePath());
        stores.add(storage);
      } catch (Exception e) {
        logger.error("Can not get the storage for {}", file.getAbsoluteFile(), e);
        continue;
      }

      logger.warn("created file: {} size: {} ", file.getAbsoluteFile(), storage.size());
    }
    return stores;
  }

  /**
   * Get storage from a file.
   */
  public static Storage getSingleFileStorage(File storageFile, FileStorageFactory factory)
      throws StorageException {
    return factory.setFile(storageFile).generate(storageFile.getAbsolutePath());
  }

  public static Storage getDummyStorage(int size) throws StorageException {
    Random random = new Random();
    String id = "DummyStorage-" + 0 + "(size:" + size + ")" + random.nextInt(100000);
    return getDummyStorage(id, size);
  }

  public static Storage getDummyStorage(String id, int size) throws StorageException {
    DummyStorageFactory factory = new DummyStorageFactory();
    logger.info("created dummpy storage: {}", id);
    return factory.setSize(size).generate(id);
  }

  public static List<Storage> getDummyStorage(int count, int size) throws StorageException {
    List<Storage> stores = new LinkedList<Storage>();
    for (int i = 0; i < count; i++) {
      String id = "DummyStorage-" + i + "(size:" + size + ")";
      stores.add(getDummyStorage(id, size));
      logger.info("created dummpy storage: {}", id);
    }
    return stores;
  }

  public static List<Storage> getAsyncDummyStorage(int count, int size) throws StorageException {
    List<Storage> stores = new LinkedList<Storage>();
    for (int i = 0; i < count; i++) {
      String id = "AsyncDummyStorage-" + i + "(size:" + size + ")";
      stores.add(new AsyncDummyStorage(getDummyStorage(id, size)));
      logger.info("created dummpy storage: {}", id);
    }
    return stores;
  }

  public static Storage getAsyncDummyStorage(int size) throws StorageException {
    return new AsyncDummyStorage(getDummyStorage(size));
  }

  public static class AsyncDummyStorage extends AsyncStorage {
    private Storage storage;

    public AsyncDummyStorage(Storage storage) {
      super("async dummy");
      this.storage = storage;
    }

    @Override
    public void read(long pos, byte[] dstBuf, int off, int len) throws StorageException {
      storage.read(pos, dstBuf, off, len);
    }
    
    @Override
    public void read(long pos, ByteBuffer buffer) throws StorageException {
      storage.read(pos, buffer);
    }

    @Override
    public <T> void read(ByteBuffer buffer, long pos, T attachment,
        CompletionHandler<Integer, ? super T> handler)
        throws StorageException {
      int size = buffer.remaining();
      try {
        storage.read(pos, buffer);
        handler.completed(size, attachment);
      } catch (Exception e) {
        handler.failed(e, attachment);
      }
    }

    @Override
    public <T> void write(ByteBuffer buffer, long pos, T attachment,
        CompletionHandler<Integer, ? super T> handler)
        throws StorageException {
      int size = buffer.remaining();
      try {
        storage.write(pos, buffer);
        handler.completed(size, attachment);
      } catch (Exception e) {
        handler.failed(e, attachment);
      }
    }

    @Override
    public void write(long pos, byte[] buf, int off, int len) throws StorageException {
      storage.write(pos, buf, off, len);
    }

    @Override
    public void write(long pos, ByteBuffer buffer) throws StorageException {
      storage.write(pos, buffer);
    }

    @Override
    public long size() {
      return storage.size();
    }
  }
}
