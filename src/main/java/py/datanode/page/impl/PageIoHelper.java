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

import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.archive.page.PageAddress;
import py.datanode.page.Page;
import py.exception.ChecksumMismatchedException;
import py.exception.DiskBrokenException;
import py.exception.StorageException;
import py.function.Callback;
import py.storage.PriorityStorage;
import py.storage.Storage;
import py.storage.impl.AsyncStorage;

public class PageIoHelper {
  private static final Logger logger = LoggerFactory.getLogger(PageIoHelper.class);

  public static void loadFromStorage(PageAddress pageAddress, Page page, Callback callback,
      boolean needLoadMetadta)
      throws StorageException {
    logger.debug(" get an page {},page address {}", page, pageAddress);
    final Storage storage = pageAddress.getStorage();
    Validate.isTrue(!page.isDirty());
    Validate.isTrue(!(pageAddress instanceof BogusPageAddress));

    ByteBuffer buffer = null;
    if (needLoadMetadta) {
      buffer = page.getIoBuffer();
    } else {
      buffer = page.getDataBuffer();
    }
    int bufferCapacity = buffer.remaining();

    CompletionHandler<Integer, Page> handler = new CompletionHandler<Integer, Page>() {
      @Override
      public void completed(Integer result, Page attachment) {
        try {
          logger.debug("read data from storage, page address: {}", pageAddress);
          if (bufferCapacity != result.intValue()) {
            callback.failed(new StorageException(
                "read data count: " + result + ", expected: " + bufferCapacity + ", PageAddress: "
                    + pageAddress)
                .setOffsetAndLength(pageAddress.getPhysicalOffsetInArchive(), bufferCapacity));
            return;
          }

          if (needLoadMetadta) {
            if (page.checkMetadata(pageAddress)) {
              page.setClean(false);
            } else {
             
             
              page.changeAddress(pageAddress);
              page.getDataBuffer().put(PageManagerImpl.ZERO_BUFFER);
              page.setClean(true);
            }
            callback.completed();
          } else {
            page.changeAddress(pageAddress);
            callback.completed();
          }

        } catch (Exception e) {
          logger.error("caught an exception", e);
        } finally {
          logger.info("nothing need to do here");
        }
      }

      @Override
      public void failed(Throwable exc, Page attachment) {
        try {
          if (exc instanceof StorageException) {
            callback.failed(exc);
          } else {
            callback.failed(new StorageException("can not load the datum from " + attachment, exc));
          }
        } catch (Exception e) {
          logger.error("caught an exception", e);
        } finally {
          logger.info("nothing need to do here");
        }
      }
    };

    long startTime = System.currentTimeMillis();
    if (storage instanceof PriorityStorage) {
      ((PriorityStorage) storage)
          .read(buffer, pageAddress.getPhysicalOffsetInArchive(), page, handler,
              PriorityStorage.Priority.MIDDLE);
    } else if (storage instanceof AsyncStorage) {
      ((AsyncStorage) storage)
          .read(buffer, pageAddress.getPhysicalOffsetInArchive(), page, handler);
    } else {
      throw new IllegalArgumentException("unsupported storage" + storage);
    }
    long endTime = System.currentTimeMillis();

    if (endTime - startTime > 1000) {
      logger
          .warn("cost time exceed time={}, pageContext={} for reading", endTime - startTime, page);
    }
  }

  public static void flushToStorage(PageAddress pageAddress, ByteBuffer buffer, Callback callback)
      throws StorageException {
    Validate.isTrue(!(pageAddress instanceof BogusPageAddress));
    Storage storage = pageAddress.getStorage();

    if (storage.isClosed() || storage.isBroken()) {
      
      logger.warn(
          "page: {}  was dirty. but can not be flushed since the storage:{} "
              + "has broken({}), closed({})",
          pageAddress, storage, storage.isBroken(), storage.isClosed());
      callback.failed(new DiskBrokenException(
          "storage is broken(" + storage.isBroken() + ") or closed(" + storage.isClosed() + ")"));
      return;
    }

    logger.debug("page: {} was dirty. go to flush the page", pageAddress);
    int writeSize = buffer.remaining();

    CompletionHandler<Integer, PageAddress> hdl = new CompletionHandler<Integer, PageAddress>() {
      @Override
      public void completed(Integer result, PageAddress attachment) {
        try {
          logger.debug("write data to storage, page address: {}", pageAddress);
          if (writeSize != result.intValue()) {
            callback.failed(new StorageException(
                "write data count: " + result + ", expected: " + writeSize + ", PageAddress: "
                    + pageAddress));
            return;
          }

          callback.completed();
        } catch (Exception e) {
          logger.error("caught an exception", e);
        } finally {
          logger.info("nothing need to do here");
        }
      }

      @Override
      public void failed(Throwable exc, PageAddress attachment) {
        try {
          if (exc instanceof StorageException) {
            callback.failed(exc);
          } else {
            callback.failed(new StorageException("can not load the datum from " + attachment, exc));
          }
        } catch (Exception e) {
          logger.error("caught an exception", e);
        } finally {
          logger.info("nothing need to do here");
        }
      }
    };

    if (storage instanceof PriorityStorage) {
      ((PriorityStorage) storage)
          .write(buffer, pageAddress.getPhysicalOffsetInArchive(), pageAddress, hdl,
              PriorityStorage.Priority.MIDDLE);
    } else if (storage instanceof AsyncStorage) {
      ((AsyncStorage) storage)
          .write(buffer, pageAddress.getPhysicalOffsetInArchive(), pageAddress, hdl);
    } else {
      throw new IllegalArgumentException("unsupported storage" + storage);
    }
  }
}
