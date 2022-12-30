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

import com.google.common.collect.RangeSet;
import java.nio.ByteBuffer;
import java.util.List;
import py.archive.page.PageAddress;
import py.common.FastBuffer;
import py.exception.ChecksumMismatchedException;
import py.exception.StorageException;

public interface Page {
  public PageAddress getAddress();

  public boolean isDirty();

  public void setDirty(boolean dirty);

  public PageStatus getPageStatus();

  public void setPageStatus(PageStatus pageStatus);

  public boolean isCachedForRead();

  public void setCachedForRead(boolean isCachedForRead);

  public boolean isPageLoaded();

  public void setPageLoaded(boolean isPageLoaded);

  public boolean isClean();

  public void setClean(boolean clean);

  public boolean canRead();

  public boolean canWrite();

  public boolean canFlush();

  public void setCanbeFlushed();

  public int checkout();

  public int checkin();

  public int getCheckoutCount();

  public void changeAddress(PageAddress pageAddress);

  public ByteBuffer getReadOnlyView();

  public ByteBuffer getReadOnlyView(int offset, int length);

  public void getData(int pageOffset, byte[] dst, int offset, int length);

  public void getData(int pageOffset, ByteBuffer byteBuffer);

  public ByteBuffer getDataBuffer();

  public int getPageSize();

  public int getPhysicalPageSize();

  public ByteBuffer getIoBuffer();

  public void write(int offset, ByteBuffer src);

  public void write(int offset, FastBuffer src, RangeSet<Integer> rangesInSrc);

  public void write(int offset, FastBuffer src);

  public boolean checkMetadata(PageAddress pageAddress);

  public boolean isFlushing();

  public void addPageListener(PageListener pageListener);

  public List<PageListener> getPageListeners();

  public void removeListeners();
}
