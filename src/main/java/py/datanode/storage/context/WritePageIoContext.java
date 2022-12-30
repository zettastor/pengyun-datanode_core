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

import javax.annotation.Nonnull;
import py.archive.ArchiveOptions;
import py.datanode.page.Page;
import py.function.Callback;

public class WritePageIoContext extends StorageIoContext implements Comparable<WritePageIoContext> {
  private final Page page;
  private final boolean external;
  private long startTime;

  public WritePageIoContext(Callback callback, Page page, boolean external) {
    super(StorageIoType.WRITE, callback, page.getAddress().getPhysicalOffsetInArchive(),
        (int) ArchiveOptions.PAGE_PHYSICAL_SIZE);
    this.page = page;
    this.external = external;
  }

  @Override
  public long getOffsetOnArchive() {
    return page.getAddress().getPhysicalOffsetInArchive();
  }

  @Override
  public long getStartTime() {
    return startTime;
  }

  public void setStartTime(long startTime) {
    this.startTime = startTime;
  }

  @Override
  public boolean markProcessing() {
    if (!page.isDirty() || !page.canFlush()) {
      return false;
    }

    if (!page.isDirty()) {
      page.setCanbeFlushed();
      return false;
    } else {
      return true;
    }

  }

  public Page getPage() {
    return page;
  }

  @Override
  public String toString() {
    return "WritePageIOContext{" + "page=" + page + ", external=" + external + ", startTime="
        + startTime + '}';
  }

  public boolean isExternal() {
    return external;
  }

  @Override
  public int compareTo(@Nonnull WritePageIoContext o) {
    if (this.isExternal() ^ o.isExternal()) {
      return this.isExternal() ? -1 : 1;
    } else {
      return Long.compare(this.getOffsetOnArchive(), o.getOffsetOnArchive());
    }
  }
}
