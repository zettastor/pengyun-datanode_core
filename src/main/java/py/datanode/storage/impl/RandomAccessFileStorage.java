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
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.exception.StorageException;
import py.storage.Storage;

/**
 * Right now, this implementation is not thread safe, and we are not using nio to read the file.
 */
public class RandomAccessFileStorage extends Storage {
  private static final Logger logger = LoggerFactory.getLogger(RandomAccessFileStorage.class);
  private RandomAccessFile file;
  private long size;

  public RandomAccessFileStorage(String id, RandomAccessFile file) throws StorageException {
    super(id);
    this.file = file;
    size = findActualSize();
  }

  public RandomAccessFileStorage(String id, File fileName) throws StorageException {
    super(id);
    open(fileName);
    size = findActualSize();
  }

  public void read(long pos, byte[] buf, int off, int len) throws StorageException {
    ByteBuffer buffer = ByteBuffer.wrap(buf, off, len);
    read(pos, buffer);
  }

  public void read(long offset, ByteBuffer byteBuffer) throws StorageException {
    int capacity = byteBuffer.capacity();
    int position = byteBuffer.position();
    int limit = byteBuffer.limit();
    logger.debug("read at {} length is {} ", offset, byteBuffer.remaining());
    try {
      file.getChannel().read(byteBuffer, offset);
    } catch (Throwable throwable) {
      throw new StorageException(
          "getWritePageCount:" + capacity + " offset:" + offset + " limit:" + limit + " position:"
              + position,
          throwable).setIoException(throwable instanceof IOException);
    }
  }

  public void write(long offset, ByteBuffer byteBuffer) throws StorageException {
    int capacity = byteBuffer.capacity();
    int position = byteBuffer.position();
    int limit = byteBuffer.limit();
    logger.debug("write at {} length is {} ", offset, byteBuffer.remaining());

    try {
      file.getChannel().write(byteBuffer, offset);
      logger.debug("buf: {} ", byteBuffer.toString());
    } catch (Throwable throwable) {
      throw new StorageException(
          "getWritePageCount:" + capacity + " offset=" + offset + " limit=" + limit + " position="
              + position,
          throwable).setIoException(throwable instanceof IOException);
    }
  }

  /**
   * Writes are synchronous.
   */
  public void write(long pos, byte[] buf, int off, int len) throws StorageException {
    ByteBuffer buffer = ByteBuffer.wrap(buf, off, len);
    write(pos, buffer);
  }
  
  private void open(File f) throws StorageException {
    try {
      file = new RandomAccessFile(f, "rws");
    } catch (Exception e) {
      logger.error("can't create a backend file for the storage", e);
      throw new StorageException(e);
    }
  }

  /**
   * Open the storage.
   */
  public void open() throws StorageException {
    throw new NotImplementedException("not supporting open function");
  }

  public void close() throws StorageException {
    try {
      file.close();
      super.close();
    } catch (Exception e) {
      logger.error("failed to close the storage", e);
      throw new StorageException(e);
    }
  }

  public long size() {
    return size;
  }

  private long findActualSize() {
    byte[] buf = new byte[1];

    long claimedFileSize = -1;
    try {
      claimedFileSize = file.length();
    } catch (Exception e) {
      logger.error("cannot read the file size. file id:{}", identifier, e);
      return 0;
    }

    if (claimedFileSize <= 0) {
      logger.warn("file:{} length:{}", identifier, claimedFileSize);
      return 0;
    }

    try {
      read(claimedFileSize - 1, buf, 0, 1);
      return claimedFileSize;
    } catch (Exception e) {
      logger.error("cannot read the entire file, size: " + claimedFileSize, e);
    }

    long maxSize = 1;
    boolean done = false;
    while (!done) {
      maxSize *= 2;
      try {
        ByteBuffer byteBuffer = ByteBuffer.wrap(buf);
        int count = file.getChannel().read(byteBuffer, maxSize);
        if (count < 0) {
          done = true;
        }
      } catch (Exception e) {
        done = true;
      }
    }

    long lo = 0;
    long hi = maxSize;
    while (lo + 1 < hi) {
      long mid = (hi + lo) / 2;
      try {
        read(mid, buf, 0, 1);
        lo = mid;
      } catch (Exception e) {
        hi = mid;
      }
    }

    return (lo + 1);
  }
}
