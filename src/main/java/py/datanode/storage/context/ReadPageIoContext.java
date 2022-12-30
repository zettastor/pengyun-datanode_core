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

package py.datanode.storage.context;

import py.archive.ArchiveOptions;
import py.archive.page.PageAddress;
import py.datanode.page.Page;
import py.function.Callback;

public class ReadPageIoContext extends StorageIoContext {
  private final Page page;
  private final boolean external;
  private final PageAddress pageAddress;

  public ReadPageIoContext(Callback callback, PageAddress pageAddress, Page page,
      boolean external) {
    super(StorageIoType.READ, callback, pageAddress.getPhysicalOffsetInArchive(),
        (int) ArchiveOptions.PAGE_PHYSICAL_SIZE);
    this.page = page;
    this.external = external;
    this.pageAddress = pageAddress;
  }

  @Override
  public boolean markProcessing() {
    return true;
  }

  @Override
  public long getOffsetOnArchive() {
    return pageAddress.getPhysicalOffsetInArchive();
  }

  @Override
  public long getStartTime() {
    return 0;
  }

  public Page getPage() {
    return page;
  }

  public boolean isExternal() {
    return external;
  }

  @Override
  public String toString() {
    return "ReadPageIOContext{" + "page=" + page + ", external=" + external + ", pageAddress="
        + pageAddress + '}';
  }

  public PageAddress getPageAddress() {
    return pageAddress;
  }
}
