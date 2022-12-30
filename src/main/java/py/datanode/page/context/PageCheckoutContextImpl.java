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

import java.util.concurrent.CountDownLatch;
import py.archive.page.PageAddress;
import py.datanode.page.Page;
import py.datanode.page.TaskType;

public class PageCheckoutContextImpl<T extends Page> extends AbstractGetPageContext<T> {
  private final CountDownLatch latch;

  public PageCheckoutContextImpl(PageAddress pageAddressToLoad, TaskType taskType,
      CountDownLatch latch) {
    super(pageAddressToLoad, taskType);
    this.latch = latch;
  }

  public PageCheckoutContextImpl(PageAddress pageAddressToLoad, TaskType taskType) {
    this(pageAddressToLoad, taskType, null);
  }

  @Override
  public void waitFor() throws InterruptedException {
    if (latch != null) {
      latch.await();
    }
  }

  @Override
  public void done() {
    if (latch != null) {
      latch.countDown();
    }
  }

  @Override
  public String toString() {
    return "PageCheckoutContextImpl [hash=" + hashCode() + ", super=" + super.toString() + "]";
  }
}
