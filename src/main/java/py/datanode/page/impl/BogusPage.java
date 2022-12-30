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

package py.datanode.page.impl;

import com.google.common.collect.RangeSet;
import java.nio.ByteBuffer;
import java.util.List;
import org.apache.commons.lang3.NotImplementedException;
import py.archive.page.PageAddress;
import py.common.FastBuffer;
import py.datanode.page.Page;
import py.datanode.page.PageListener;
import py.datanode.page.PageStatus;
import py.exception.ChecksumMismatchedException;

public class BogusPage implements Page {
  private final PageAddress pageAddress;

  public BogusPage(PageAddress pageAddress) {
    this.pageAddress = pageAddress;
  }

  @Override
  public PageAddress getAddress() {
    return pageAddress;
  }

  @Override
  public boolean isDirty() {
    return false;
  }

  @Override
  public void setDirty(boolean dirty) {
    throw new NotImplementedException("This is a bogus page");
  }

  @Override
  public int checkout() {
    throw new NotImplementedException("This is a bogus page");
  }

  @Override
  public int checkin() {
    throw new NotImplementedException("This is a bogus page");
  }

  @Override
  public void changeAddress(PageAddress pageAddress) {
    throw new NotImplementedException("This is a bogus page");
  }
  
  @Override
  public ByteBuffer getReadOnlyView() {
    throw new NotImplementedException("This is a bogus page");
  }
  
  @Override
  public ByteBuffer getReadOnlyView(int offset, int length) {
    throw new NotImplementedException("This is a bogus page");
  }

  @Override
  public void write(int offset, ByteBuffer src) {
    throw new NotImplementedException("This is a bogus page");
  }

  @Override
  public void write(int offset, FastBuffer src, RangeSet<Integer> rangesToApplyLog) {
    throw new NotImplementedException("This is bogus page");
  }
  
  @Override
  public void write(int offset, FastBuffer src) {
    throw new NotImplementedException("This is a bogus page");
  }

  @Override
  public ByteBuffer getDataBuffer() {
    throw new NotImplementedException("This is bogus page");
  }

  @Override
  public int getCheckoutCount() {
    return 0;
  }

  @Override
  public void getData(int pageOffset, byte[] dst, int offset, int length) {
    throw new NotImplementedException("This is bogus page");
  }

  @Override
  public void getData(int pageOffset, ByteBuffer byteBuffer) {
    throw new NotImplementedException("This is bogus page");
  }

  @Override
  public boolean isFlushing() {
    throw new NotImplementedException("This is bogus page");
  }

  @Override
  public PageStatus getPageStatus() {
    throw new NotImplementedException("This is a bogus page");
  }

  @Override
  public void setPageStatus(PageStatus pageStatus) {
    throw new NotImplementedException("This is a bogus page");
  }

  @Override
  public boolean canRead() {
    throw new NotImplementedException("This is a bogus page");
  }

  @Override
  public boolean canWrite() {
    throw new NotImplementedException("This is a bogus page");
  }

  @Override
  public boolean canFlush() {
    throw new NotImplementedException("This is a bogus page");
  }

  @Override
  public void setCanbeFlushed() {
    throw new NotImplementedException("This is a bogus page");
  }

  @Override
  public boolean isCachedForRead() {
    throw new NotImplementedException("This is a bogus page");
  }

  @Override
  public void setCachedForRead(boolean isCachedForRead) {
    throw new NotImplementedException("This is a bogus page");
  }

  @Override
  public ByteBuffer getIoBuffer() {
    throw new NotImplementedException("This is a bogus page");
  }

  @Override
  public boolean checkMetadata(PageAddress pageAddress) {
    throw new NotImplementedException("This is a bogus page");
  }

  @Override
  public boolean isPageLoaded() {
    throw new NotImplementedException("This is a bogus page");
  }

  @Override
  public void setPageLoaded(boolean isPageLoaded) {
    throw new NotImplementedException("This is a bogus page");
  }

  @Override
  public void addPageListener(PageListener pageListener) {
    throw new NotImplementedException("This is a bogus page");
  }

  @Override
  public List<PageListener> getPageListeners() {
    throw new NotImplementedException("This is a bogus page");
  }

  @Override
  public void removeListeners() {
    throw new NotImplementedException("This is a bogus page");
  }

  @Override
  public int getPhysicalPageSize() {
    throw new NotImplementedException("This is a bogus page");
  }

  @Override
  public int getPageSize() {
    throw new NotImplementedException("This is a bogus page");
  }

  @Override
  public boolean isClean() {
    throw new NotImplementedException("This is a bogus page");
  }

  @Override
  public void setClean(boolean clean) {
    throw new NotImplementedException("This is a bogus page");
  }

}
