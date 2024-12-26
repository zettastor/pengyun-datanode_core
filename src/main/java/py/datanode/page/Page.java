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
