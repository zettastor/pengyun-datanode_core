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

import java.util.List;
import py.archive.page.PageAddress;
import py.datanode.page.IoType;
import py.datanode.page.Page;
import py.datanode.page.PageListener;
import py.storage.Storage;

public class PageInnerFlushContextImpl<T extends Page> extends AbstractBasePageContext<T> {
  private T page;
  private IoType ioType;

  public PageInnerFlushContextImpl(IoType ioType, T page) {
    super(null);
    this.ioType = ioType;
    this.page = page;
  }

  @Override
  public PageAddress getPageAddressForIo() {
    return getPage().getAddress();
  }

  @Override
  public Storage getStorage() {
    return getPage().getAddress().getStorage();
  }

  @Override
  public void done() {
   
    List<PageListener> pageListeners = page.getPageListeners();
    if (pageListeners.size() == 0) {
      return;
    }

    try {
      if (isSuccess()) {
        for (PageListener listener : pageListeners) {
          listener.successToPersist();
        }
      } else {
        for (PageListener listener : pageListeners) {
          listener.failToPersist(getCause());
        }
      }
    } finally {
      page.removeListeners();
    }
  }

  @Override
  public T getPage() {
    return page;
  }

  @Override
  public void setPage(T page) {
    this.page = page;
  }

  @Override
  public IoType getIoType() {
    return ioType;
  }

  @Override
  public void setIoType(IoType ioType) {
    this.ioType = ioType;
  }

  @Override
  public String toString() {
    return "PageInnerFlushContextImpl [super=" + super.toString() + ", ioType=" + ioType + ", page="
        + page + "]";
  }

}
