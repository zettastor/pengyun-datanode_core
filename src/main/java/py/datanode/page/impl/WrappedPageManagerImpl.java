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

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.archive.page.PageAddress;
import py.datanode.configuration.DataNodeConfiguration;
import py.datanode.page.AvailablePageLister;
import py.datanode.page.DirtyPagePoolFactory;
import py.datanode.page.Page;
import py.datanode.page.PageContext;
import py.datanode.page.PageIoListener;
import py.datanode.page.PageManager;
import py.datanode.page.context.PageContextWrapper;
import py.datanode.storage.scheduler.StorageIoWorkerHouseKeeper;

public class WrappedPageManagerImpl implements PageManager<Page>, PageIoListener {
  private static final Logger logger = LoggerFactory.getLogger(WrappedPageManagerImpl.class);

  private final AtomicBoolean closed = new AtomicBoolean(true);
  private final DataNodeConfiguration cfg;

  private final StorageIoDispatcher storageIoDispatcher;

  private final StorageIoWorkerHouseKeeper storageIoWorkerHouseKeeper;

  private final DirtyPagePoolFactory dirtyPagePoolFactory;

  private final AvailablePageLister availablePageLister;

  private final PageManagerDispatcher pageManagerDispatcher;

  private final List<PageManagerImpl> pageManagers;

  WrappedPageManagerImpl(DataNodeConfiguration cfg,
      StorageIoWorkerHouseKeeper storageIoWorkerHouseKeeper,
      DirtyPagePoolFactory dirtyPagePoolFactory,
      AvailablePageLister availablePageLister, StorageIoDispatcher storageIoDispatcher,
      PageManagerDispatcher pageManagerDispatcher, List<PageManagerImpl> pageManagers) {
    this.cfg = cfg;
    this.storageIoDispatcher = storageIoDispatcher;
    this.pageManagerDispatcher = pageManagerDispatcher;
    this.pageManagers = pageManagers;

    this.storageIoWorkerHouseKeeper = storageIoWorkerHouseKeeper;
    this.availablePageLister = availablePageLister;
    this.dirtyPagePoolFactory = dirtyPagePoolFactory;
  }

  private PageManagerImpl dispatch(PageAddress address) {
    return pageManagerDispatcher.select(pageManagers, address);
  }

  @Override
  public void checkout(PageContext<Page> pageContext) {
    if (pageContext instanceof PageContextWrapper) {
      PageContextWrapper<Page> pageContextWrapper = (PageContextWrapper<Page>) pageContext;
      pageContextWrapper.getPageContexts().forEach(this::checkout);
    } else {
      dispatch(pageContext.getPageAddressForIo()).checkout(pageContext);
    }
  }

  @Override
  public PageContext<Page> checkoutForRead(PageAddress address) {
    return dispatch(address).checkoutForRead(address);
  }

  @Override
  public PageContext<Page> checkoutForExternalRead(PageAddress address) {
    return dispatch(address).checkoutForExternalRead(address);
  }

  @Override
  public PageContext<Page> checkoutForInternalWrite(PageAddress address) {
    return dispatch(address).checkoutForInternalWrite(address);
  }

  @Override
  public PageContext<Page> checkoutForExternalWrite(PageAddress address) {
    return dispatch(address).checkoutForExternalWrite(address);
  }

  @Override
  public PageContext<Page> checkoutForInternalCorrection(PageAddress address) {
    return dispatch(address).checkoutForInternalCorrection(address);
  }

  @Override
  public PageContext<Page> checkoutForExternalCorrection(PageAddress address) {
    return dispatch(address).checkoutForExternalCorrection(address);
  }

  @Override
  public void checkin(PageContext<Page> pageContext) {
    if (pageContext instanceof PageContextWrapper) {
      PageContextWrapper<Page> pageContextWrapper = (PageContextWrapper<Page>) pageContext;
      pageContextWrapper.getPageContexts().forEach(this::checkin);
    } else {
      if (dispatch(pageContext.getPageAddressForIo()) == null) {
        logger.error("got a null page manager {}, {}", pageContext,
            pageContext.getPageAddressForIo());
      } else {
        dispatch(pageContext.getPageAddressForIo()).checkin(pageContext);
      }
    }
  }

  @Override
  public void flushPage(PageContext<Page> pageContext) {
    if (pageContext instanceof PageContextWrapper) {
      PageContextWrapper<Page> pageContextWrapper = (PageContextWrapper<Page>) pageContext;
      pageContextWrapper.getPageContexts().forEach(this::flushPage);
    } else {
      dispatch(pageContext.getPageAddressForIo()).flushPage(pageContext);
    }
  }

  @Override
  public int recordFlushError(PageAddress address) {
    return dispatch(address).recordFlushError(address);
  }

  @Override
  public long getTotalPageCount() {
    int count = 0;
    for (PageManager<Page> p : pageManagers) {
      count += p.getTotalPageCount();
    }
    return count;
  }

  @Override
  public long getFreePageCount() {
    int count = 0;
    for (PageManager<Page> p : pageManagers) {
      count += p.getFreePageCount();
    }
    return count;
  }

  @Override
  public long getDirtyPageCount() {
    int count = 0;
    for (PageManager<Page> p : pageManagers) {
      count += p.getDirtyPageCount();
    }
    return count;
  }

  @Override
  public void start() {
    synchronized (closed) {
      if (closed.compareAndSet(true, false)) {
        for (PageManager<Page> p : pageManagers) {
          p.start();
        }
      }
      storageIoDispatcher
          .start(this, storageIoWorkerHouseKeeper, dirtyPagePoolFactory, availablePageLister);
    }
  }

  @Override
  public void close() throws InterruptedException {
    synchronized (closed) {
      if (closed.compareAndSet(false, true)) {
        for (PageManager<Page> p : pageManagers) {
          p.close();
        }
      }
      storageIoDispatcher.stop();
    }
  }

  @Override
  public boolean isClosed() {
    return closed.get();
  }

  @Override
  public DataNodeConfiguration getCfg() {
    return cfg;
  }

  @Override
  public boolean cleanCache() {
    boolean cleaned = true;
    for (PageManager<Page> p : pageManagers) {
      cleaned &= p.cleanCache();
    }
    return cleaned;
  }

  @Override
  public StorageIoDispatcher getStorageIoDispatcher() {
    return storageIoDispatcher;
  }

  @Override
  public void loadedFromStorage(PageContext<Page> pageContext) {
    if (pageContext instanceof PageContextWrapper) {
      PageContextWrapper<Page> pageContextWrapper = (PageContextWrapper<Page>) pageContext;
      pageContextWrapper.getPageContexts().forEach(this::loadedFromStorage);
    } else {
      dispatch(pageContext.getPageAddressForIo()).loadedFromStorage(pageContext);
    }
  }

  @Override
  public void flushedToStorage(PageContext<Page> context) {
    if (context instanceof PageContextWrapper) {
      PageContextWrapper<Page> pageContextWrapper = (PageContextWrapper<Page>) context;
      pageContextWrapper.getPageContexts().forEach(this::flushedToStorage);
    } else {
      dispatch(context.getPageAddressForIo()).flushedToStorage(context);
    }
  }

  @Override
  public void flushedToL2Write(PageContext<Page> context) {
    if (context instanceof PageContextWrapper) {
      PageContextWrapper<Page> pageContextWrapper = (PageContextWrapper<Page>) context;
      pageContextWrapper.getPageContexts().forEach(this::flushedToL2Write);
    } else {
      dispatch(context.getPageAddressForIo()).flushedToL2Write(context);
    }
  }
}
