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

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import py.archive.page.PageAddress;
import py.datanode.page.Page;
import py.datanode.page.PageContext;
import py.datanode.page.PageContextCallback;
import py.datanode.page.TaskType;
import py.datanode.page.impl.GarbagePageAddress;

public class AsyncShadowPageContextImpl<T extends Page> extends PageContextWrapper<T> {
  private final PageContext<T> originalPageContext;
  private final PageContext<T> shadowPageContext;
  private AtomicInteger responseCount;
  private PageContextCallback<T> callback;

  public AsyncShadowPageContextImpl(PageAddress sourceAddress, PageAddress destAddress,
      PageContextCallback<T> callback) {
    super(new ArrayList<PageContext<T>>());
    this.callback = callback;
    this.responseCount = new AtomicInteger(0);

    this.originalPageContext = new AsyncPageCheckoutContextImpl<T>(sourceAddress,
        TaskType.CHECK_OUT_FOR_READ,
        null) {
      public void done() {
        doneMyself();
      }
    };

    pageContexts.add(originalPageContext);
    this.responseCount.incrementAndGet();
    if (!(destAddress instanceof GarbagePageAddress)) {
      this.shadowPageContext = new AsyncPageCheckoutContextImpl<T>(destAddress,
          TaskType.CHECK_OUT_FOR_EXTERNAL_CORRECTION,
          null) {
        public void done() {
          doneMyself();
        }
      };
      pageContexts.add(shadowPageContext);
      this.responseCount.incrementAndGet();
    } else {
      shadowPageContext = new BogusPageContext<>();
      shadowPageContext.isSuccess();
    }
  }

  @Override
  public PageAddress getPageAddressForIo() {
    return originalPageContext.getPageAddressForIo();
  }

  private void doneMyself() {
    if (responseCount.decrementAndGet() == 0) {
      callback.completed(this);
    }
  }

  @Override
  public void setExpiredTime(long expiredTime) {
    originalPageContext.setExpiredTime(expiredTime);
    shadowPageContext.setExpiredTime(expiredTime);
  }

  public PageContext<T> getOriginalPageContext() {
    return originalPageContext;
  }

  public PageContext<T> getShadowPageContext() {
    return shadowPageContext;
  }

  @Override
  public boolean isSuccess() {
    if (originalPageContext.isSuccess()) {
      if (shadowPageContext instanceof BogusPageContext) {
        return true;
      }
      return shadowPageContext.isSuccess();
    } else {
      return false;
    }
  }

  @Override
  public String toString() {
    return "AsyncShadowPageContextImpl [originalPageContext=" + originalPageContext
        + ", shadowPageContext="
        + shadowPageContext + ", responseCount=" + responseCount + ", waiter=" + callback + "]";
  }
}
