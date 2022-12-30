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

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.lang.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.common.DirectAlignedBufferAllocator;
import py.exception.StorageException;
import py.storage.Storage;

public class PageAlignedStorage extends Storage {
  public static final int DEFAULT_SECTOR_SIZE = 512;
  private static final Logger logger = LoggerFactory.getLogger(PageAlignedStorage.class);
  private final long sectorSize;
  private final RandomAccessFile raf;
  private final long fileSize;
  private final AtomicBoolean closed = new AtomicBoolean(false);

  public PageAlignedStorage(String id, File file, int sectorSize, String mode)
      throws StorageException {
    super(id);
    try {
      raf = new RandomAccessFile(file, mode);
      this.sectorSize = sectorSize;
      this.fileSize = findUsableFileSize();
    } catch (IOException e) {
      throw new StorageException(e);
    }
  }

  public PageAlignedStorage(String id, RandomAccessFile randomAccessFile, int sectorSize)
      throws StorageException {
    super(id);
    this.raf = randomAccessFile;
    this.sectorSize = sectorSize;
    this.fileSize = findUsableFileSize();
  }

  public PageAlignedStorage(String id, File file) throws StorageException {
    this(id, file, "rw");
  }

  public PageAlignedStorage(String id, File file, String mode) throws StorageException {
    this(id, file, DEFAULT_SECTOR_SIZE, mode);
  }

  public PageAlignedStorage(String id, RandomAccessFile file) throws StorageException {
    this(id, file, DEFAULT_SECTOR_SIZE);
  }

  public long getSectorSize() {
    return sectorSize;
  }

  public long size() {
    return (fileSize);
  }

  @Override
  public void read(long pos, byte[] dstBuf, int off, int len) throws StorageException {
    throw new NotImplementedException("read");
  }
  
  
  public void read(long offset, ByteBuffer byteBuffer) throws StorageException {
    checkAligned(byteBuffer.remaining());
    checkValidOffset(offset);

    int remaining = byteBuffer.remaining();
    int capacity = byteBuffer.capacity();
    int position = byteBuffer.position();
    int limit = byteBuffer.limit();
    int count = 0;
    try {
      count = raf.getChannel().read(byteBuffer, offset);
      if (count < 0) {
        throw new EOFException();
      }
      if (count != remaining) {
        throw new IOException(
            "did not read all bytes, read:" + count + " expected to read:" + remaining);
      }
    } catch (Throwable throwable) {
      throw new StorageException(
          "count:" + count + " getWritePageCount:" + capacity + " position:" + position + " limit:"
              + limit
              + " offset:" + offset, throwable).setIoException(throwable instanceof IOException);
    }
  }
  
  @Override
  public void write(long pos, byte[] buf, int off, int len) throws StorageException {
    throw new NotImplementedException("write");
  }

  public void write(long offset, ByteBuffer byteBuffer) throws StorageException {
    checkAligned(byteBuffer.remaining());
    checkValidOffset(offset);

    int remaining = byteBuffer.remaining();
    int capacity = byteBuffer.capacity();
    int position = byteBuffer.position();
    int limit = byteBuffer.limit();
    int count = 0;
    try {
      if (0 == offset) {
        long magic = byteBuffer.duplicate().getLong();
        if ((magic & 0x1847EBD000000000L) != 0x1847EBD000000000L) {
          logger.error("fond a invalid magic", new Exception());
        }
      }
      count = raf.getChannel().write(byteBuffer, offset);
      if (count != remaining) {
        throw new IOException(
            "did not write all bytes, write:" + count + " expected to write:" + remaining);
      }
    } catch (Throwable throwable) {
      throw new StorageException(
          "count=" + count + " getWritePageCount=" + capacity + " position=" + position + " limit="
              + limit
              + " offset=" + offset, throwable).setIoException(throwable instanceof IOException);
    }
  }

  public void close() throws StorageException {
    try {
      closed.getAndSet(true);
      logger.info("closing storage: {}  ", this);
      raf.close();
      super.close();
    } catch (Throwable t) {
      throw new StorageException(t);
    }
  }

  private long findUsableFileSize() throws StorageException {
    ByteBuffer buffer = DirectAlignedBufferAllocator
        .allocateAlignedByteBuffer((int) sectorSize, (int) sectorSize);

    long maxSize = 1;
    boolean done = false;
    while (!done) {
      maxSize *= 2;
      try {
        buffer.clear();
        int count = raf.getChannel().read(buffer, maxSize * sectorSize);
        if (count < 0) {
          done = true;
        }
      } catch (IOException ioe) {
        logger.error("caught an exception", ioe);
        done = true;
      }
    }

    long lo = 0;
    long hi = maxSize;

    while (lo + 1 < hi) {
      long mid = (hi + lo) / 2;
      try {
        buffer.clear();
        int count = raf.getChannel().read(buffer, mid * sectorSize);
        if (count < buffer.capacity()) {
          hi = mid;
        } else {
          lo = mid;
        }
      } catch (IOException ex) {
        logger.error("caught an exception", ex);
        hi = mid;
      }
    }

    return (lo + 1) * sectorSize;
  }

  private void checkValidOffset(long value) throws StorageException {
    if (value == 0) {
      return;
    }
    checkAligned(value);
  }

  private void checkAligned(long value) throws StorageException {
    if (value < sectorSize) {
      throw new StorageException(
          "size(" + value + ") must be at least as large as the sectorSize(" + sectorSize + ")");
    }
    if (value % sectorSize != 0) {
      throw new StorageException(
          "size(" + value + ") must be a multiple of the sectorSize(" + sectorSize + ")");
    }
  }

  @Override
  public void open() throws StorageException {
    throw new NotImplementedException("open");
  }
}
