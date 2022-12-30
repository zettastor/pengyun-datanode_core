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
