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

package py.datanode.page;

import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.archive.page.PageAddress;
import py.datanode.page.impl.BogusPageAddress;
import py.datanode.page.impl.PageManagerImpl;
import py.exception.ChecksumMismatchedException;
import py.exception.DiskBrokenException;
import py.exception.StorageException;
import py.storage.Storage;
import py.storage.impl.AsyncStorage;

public class PageIoHelper {
  private static final Logger logger = LoggerFactory.getLogger(PageIoHelper.class);

  public static void loadFromStorage(PageContext<Page> context, PageIoListener listener)
      throws StorageException {
    PageAddress pageAddress = context.getPageAddressForIo();
    Page page = context.getPage();
    Storage storage = pageAddress.getStorage();
    Validate.isTrue(!page.isDirty());
    Validate.isTrue(!(pageAddress instanceof BogusPageAddress));

    @SuppressWarnings("unchecked") final AsyncStorage asyncStorage = (AsyncStorage) storage;
    final ByteBuffer buffer = page.getIoBuffer();

    CompletionHandler<Integer, PageContext<Page>> handler =
        new CompletionHandler<Integer, PageContext<Page>>() {
          @Override
          public void completed(Integer result, PageContext<Page> attachment) {
            PageAddress pageAddress = attachment.getPageAddressForIo();
            try {
              if (buffer.capacity() != result.intValue()) {
                attachment.setCause(new StorageException(
                    "read data count: " + result + ", expected: " + buffer.capacity()
                        + ", PageAddress: "
                        + pageAddress)
                    .setOffsetAndLength(pageAddress.getPhysicalOffsetInArchive(),
                        buffer.capacity()));
                listener.loadedFromStorage(attachment);
                return;
              }
    
              if (page.checkMetadata(pageAddress)) {
                page.setClean(false);
              } else {
                page.changeAddress(pageAddress);
                page.getDataBuffer().put(PageManagerImpl.ZERO_BUFFER);
                page.setClean(true);
              }
    
              listener.loadedFromStorage(attachment);
            } catch (Exception e) {
              logger.error("caught an exception", e);
            } finally {
              logger.info("nothing need to do here");
            }
          }   

          @Override
          public void failed(Throwable exc, PageContext<Page> attachment) {
            try {
              attachment
                  .setCause(new StorageException("can not load the datum from " + attachment, exc));
              listener.loadedFromStorage(attachment);
            } catch (Exception e) {
              logger.error("caught an exception", e);
            } finally {
              logger.info("nothing need to do here");
            }
          }
        };

    long startTime = System.currentTimeMillis();
    asyncStorage.read(buffer, pageAddress.getPhysicalOffsetInArchive(), context, handler);
    long endTime = System.currentTimeMillis();

    if (endTime - startTime > 1000) {
      logger.warn("cost time exceed time={}, pageContext={} for reading", endTime - startTime,
          context);
    }
  }

  public static void flushToStorage(PageContext<Page> context, PageIoListener listener)
      throws StorageException {
    Page page = context.getPage();
    PageAddress pageAddress = page.getAddress();

    Validate.isTrue(!(pageAddress instanceof BogusPageAddress));
    Validate.isTrue(page.isDirty());
    Storage storage = pageAddress.getStorage();
    if (storage.isClosed() || storage.isBroken()) {
      
      logger.warn(
          "page: {}  was dirty. " 
              + "but can not be flushed since the storage:{} has broken({}), closed({})",
          pageAddress, storage, storage.isBroken(), storage.isClosed());
      context.setCause(new DiskBrokenException(
          "storage is broken(" + storage.isBroken() + ") or closed(" + storage.isClosed() + ")"));
      Validate.isTrue(page.isFlushing());
      listener.flushedToStorage(context);
      return;
    }

    @SuppressWarnings("unchecked") final AsyncStorage asyncStorage = (AsyncStorage) storage;
    final ByteBuffer buffer = page.getIoBuffer();

    CompletionHandler<Integer, PageContext<Page>> handler =
          new CompletionHandler<Integer, PageContext<Page>>() {
          @Override
          public void completed(Integer result, PageContext<Page> attachment) {
            try {
              logger.debug("write data to storage, page address: {}, address {}", page.getAddress(),
                  attachment.getPageAddressForIo());
              if (buffer.capacity() != result.intValue()) {
                attachment.setCause(new StorageException(
                    "write data count: " + result + ", expected: " + buffer.capacity()
                        + ", PageAddress: "
                        + attachment.getPage().getAddress()));
                Validate.isTrue(page.isFlushing());
                listener.flushedToStorage(context);
                return;
              }
    
              Validate.isTrue(page.isFlushing());
              listener.flushedToStorage(context);
            } catch (Exception e) {
              logger.error("caught an exception", e);
            } finally {
              logger.info("nothing need to do here");
            }
          }
    
          @Override
          public void failed(Throwable exc, PageContext<Page> attachment) {
            try {
              attachment
                  .setCause(new StorageException("can not flush the datum to " + attachment, exc));
              Validate.isTrue(page.isFlushing());
              listener.flushedToStorage(context);
            } catch (Exception e) {
              logger.error("caught an exception", e);
            } finally {
              logger.info("nothing need to do here");
            }
          }
        };

    asyncStorage.write(buffer, pageAddress.getPhysicalOffsetInArchive(), context, handler);
  }

  public static void flushToStorage(PageContext<Page> context, ByteBuffer buffer,
      PageIoListener listener) throws StorageException {
    Page page = context.getPage();
    PageAddress pageAddress = page.getAddress();

    Validate.isTrue(!(pageAddress instanceof BogusPageAddress));
    Validate.isTrue(page.isDirty());
    Storage storage = pageAddress.getStorage();
    if (storage.isClosed() || storage.isBroken()) {
      
      logger.warn(
          "page: {}  was dirty. " 
              + "but can not be flushed since the storage:{} has broken({}), closed({})",
          pageAddress, storage, storage.isBroken(), storage.isClosed());
      context.setCause(new DiskBrokenException(
          "storage is broken(" + storage.isBroken() + ") or closed(" + storage.isClosed() + ")"));
      Validate.isTrue(page.isFlushing());
      listener.flushedToStorage(context);
      return;
    }

    logger.debug("page: {} was dirty. go to flush the page", pageAddress);

    @SuppressWarnings("unchecked") final AsyncStorage asyncStorage = (AsyncStorage) storage;
    
    int remaining = buffer.remaining();
    CompletionHandler<Integer, PageContext<Page>> handler = 
          new CompletionHandler<Integer, PageContext<Page>>() {
          @Override
          public void completed(Integer result, PageContext<Page> attachment) {
            try {
              logger.debug("write data to storage, page address: {}, address {}", page.getAddress(),
                  attachment.getPageAddressForIo());
              if (remaining != result.intValue()) {
                attachment.setCause(new StorageException(
                    "write data count: " + result + ", expected: " + buffer.capacity()
                        + ", PageAddress: "
                        + attachment.getPage().getAddress()));
                Validate.isTrue(page.isFlushing());
                listener.flushedToStorage(context);
                return;
              }
    
              Validate.isTrue(page.isFlushing());
              listener.flushedToStorage(context);
            } catch (Exception e) {
              logger.error("caught an exception", e);
            } finally {
              logger.info("nothing need to do here");
            }
          }
    
          @Override
          public void failed(Throwable exc, PageContext<Page> attachment) {
            try {
              attachment
                  .setCause(new StorageException("can not flush the datum to " + attachment, exc));
              Validate.isTrue(page.isFlushing());
              listener.flushedToStorage(context);
            } catch (Exception e) {
              logger.error("caught an exception", e);
            } finally {
              logger.info("nothing need to do here");
            }
          }
        };

    long startTime = System.currentTimeMillis();
    asyncStorage.write(buffer, pageAddress.getPhysicalOffsetInArchive(), context, handler);
    long endTime = System.currentTimeMillis();

    if (endTime - startTime > 1000) {
      logger.warn("cost time exceed time={}, pageContext={} for writing", endTime - startTime,
          context);
    }
  }
}
