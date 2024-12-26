/**
* Copyright (C) 2013-2024 Nanjing Pengyun Network Technology Co., Ltd.
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
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
