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

import org.apache.commons.lang.Validate;
import py.archive.page.PageAddress;
import py.datanode.page.IoType;
import py.datanode.page.Page;
import py.datanode.page.PageContext;
import py.storage.Storage;

/**
 * when a page will be flushed to storage or loaded from storage, a new {@link #PageLoadContextImpl}
 * should be created and wrap the original {@link PageContext}.
 */
public class PageLoadContextImpl<P extends Page> extends AbstractBasePageContext<P> {
  private final PageContext<P> originalPageContext;
  private IoType ioType;

  public PageLoadContextImpl(IoType ioType, PageContext<P> originalPageContext) {
    super(null);
    this.originalPageContext = originalPageContext;
    Validate.isTrue(originalPageContext != null);
    this.ioType = ioType;
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
  public P getPage() {
    return originalPageContext.getPage();
  }

  @Override
  public void setPage(P page) {
    originalPageContext.setPage(page);
  }

  @Override
  public PageAddress getPageAddressForIo() {
    return this.originalPageContext.getPageAddressForIo();
  }

  @Override
  public PageContext<P> getOriginalPageContext() {
    return originalPageContext;
  }

  @Override
  public Storage getStorage() {
    return this.originalPageContext.getPageAddressForIo().getStorage();
  }

  @Override
  public String toString() {
    return "PageLoadContextImpl [super=" + super.toString() + ", originalPageContext="
        + originalPageContext
        + ", ioType=" + ioType + "]";
  }

}
