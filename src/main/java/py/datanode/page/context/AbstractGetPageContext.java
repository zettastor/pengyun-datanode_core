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
