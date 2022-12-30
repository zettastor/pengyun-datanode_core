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
import py.datanode.page.impl.BogusPageAddress;

public class CleanCachePageContextImpl<P extends Page> extends AbstractBasePageContext<P> {
  private final CountDownLatch latch;

  private final PageAddress pageAddress = new BogusPageAddress();

  public CleanCachePageContextImpl(TaskType taskType, CountDownLatch latch) {
    super(taskType);
    this.latch = latch;
  }

  @Override
  public PageAddress getPageAddressForIo() {
    return pageAddress;
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
}
