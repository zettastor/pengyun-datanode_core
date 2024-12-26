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
