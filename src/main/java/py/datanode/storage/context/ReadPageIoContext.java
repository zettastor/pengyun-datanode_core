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
