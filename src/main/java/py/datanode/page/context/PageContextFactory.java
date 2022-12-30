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

package py.datanode.page.context;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import py.archive.page.PageAddress;
import py.datanode.page.IoType;
import py.datanode.page.Page;
import py.datanode.page.PageContext;
import py.datanode.page.PageContextCallback;
import py.datanode.page.TaskType;
import py.storage.Storage;

public class PageContextFactory<T extends Page> {
  public PageContext<T> generateCheckoutContext(PageAddress pageAddress, TaskType taskType,
      long timeoutMs) {
    PageContext<T> context = new PageCheckoutContextImpl<T>(pageAddress, taskType,
        new CountDownLatch(1));
    context.setExpiredTime(System.currentTimeMillis() + timeoutMs);
    return context;
  }

  public PageContext<T> generateBogusContext(PageContext<T> originalPageContext) {
    return new BogusPageContext<T>(originalPageContext);
  }

  public PageContext<T> generateStorageDirtyPageContext(Storage storage) {
    return new StorageDirtyPageContextImpl<T>(storage);
  }

  public PageContext<T> generateAsyncCheckoutContext(PageAddress pageAddresses, TaskType taskType,
      PageContextCallback<T> callback, long timeoutMs) {
    PageContext<T> context = new AsyncPageCheckoutContextImpl<T>(pageAddresses, taskType, callback);
    context.setExpiredTime(System.currentTimeMillis() + timeoutMs);
    return context;
  }

  public PageContext<T> generatePageContextWrapper(Collection<PageContext<T>> pageContexts) {
    return new PageContextWrapper<T>(pageContexts);
  }

  public PageContext<T> generateInnerFlushContext(T page) {
    return new PageInnerFlushContextImpl<T>(IoType.TODISK, page);
  }

  public PageContext<T> generateExternalFlushContext(T page) {
    return new PageInnerFlushContextImpl<T>(IoType.TODISKFOREXTERNAL, page);
  }

  public PageContext<T> generatePageFlushContext(PageAddress pageAddressToFlush) {
    return generatePageFlushContext(pageAddressToFlush, null);
  }

  public PageContext<T> generatePageFlushContext(PageAddress pageAddressToFlush,
      CountDownLatch latch) {
    return new PageFlushContextImpl<T>(TaskType.FLUSH_PAGE, pageAddressToFlush, latch);
  }

  public PageContext<T> generateCleanCacheContext() {
    return new CleanCachePageContextImpl<T>(TaskType.CLEAN_CACHE, new CountDownLatch(1));
  }

  public PageContext<T> generatePageLoadContext(IoType ioType, PageContext<T> originalPageContext) {
    return new PageLoadContextImpl<T>(ioType, originalPageContext);
  }

  public PageContext<T> generateAsyncShadowPageContext(PageAddress originalAddress,
      PageAddress shadowAddress,
      PageContextCallback<T> callback, long timeoutMs) {
    PageContext<T> context = new AsyncShadowPageContextImpl<T>(originalAddress, shadowAddress,
        callback);
    context.setExpiredTime(System.currentTimeMillis() + timeoutMs);
    return context;
  }
}
