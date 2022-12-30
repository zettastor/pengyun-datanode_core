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
