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

package py.datanode.page;

import py.archive.page.PageAddress;
import py.datanode.configuration.DataNodeConfiguration;
import py.datanode.page.impl.StorageIoDispatcher;
import py.exception.StorageException;

public interface PageManager<P extends Page> {
  public void checkout(PageContext<P> pageContext);

  public PageContext<P> checkoutForRead(PageAddress address);

  public PageContext<P> checkoutForExternalRead(PageAddress address);

  public PageContext<P> checkoutForInternalWrite(PageAddress address);

  public PageContext<P> checkoutForExternalWrite(PageAddress address);

  public PageContext<P> checkoutForInternalCorrection(PageAddress address);

  public PageContext<P> checkoutForExternalCorrection(PageAddress address);

  public void checkin(PageContext<P> pageContext);

  public void flushPage(PageContext<P> pageContext);

  public int recordFlushError(PageAddress pageAddress);

  public long getTotalPageCount();

  public long getFreePageCount();

  public long getDirtyPageCount();

  void start();

  public void close() throws InterruptedException;

  public boolean isClosed();

  public DataNodeConfiguration getCfg();

  public boolean cleanCache();

  StorageIoDispatcher getStorageIoDispatcher();
}
