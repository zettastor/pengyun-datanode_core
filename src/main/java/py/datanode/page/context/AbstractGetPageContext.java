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

import py.archive.page.PageAddress;
import py.datanode.page.Page;
import py.datanode.page.TaskType;

public abstract class AbstractGetPageContext<P extends Page> extends AbstractBasePageContext<P> {
  private PageAddress pageAddressForIo;
  private long expiredTime;
  private P page;

  public AbstractGetPageContext(PageAddress pageAddressForIo, TaskType taskType) {
    super(taskType);
    this.pageAddressForIo = pageAddressForIo;
  }

  @Override
  public PageAddress getPageAddressForIo() {
    return pageAddressForIo;
  }

  @Override
  public void setExpiredTime(long expiredTime) {
    this.expiredTime = expiredTime;
  }

  @Override
  public P getPage() {
    return page;
  }

  @Override
  public void setPage(P page) {
    this.page = page;
  }

  @Override
  public boolean isExpired() {
    return System.currentTimeMillis() > expiredTime ? true : false;
  }

  @Override
  public String toString() {
    return "AbstractGetPageContext [super=" + super.toString() + ", pageAddressForIo="
        + pageAddressForIo
        + ", expiredTime=" + expiredTime + ", page=" + page + "]";
  }

}
