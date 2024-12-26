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

import java.nio.ByteBuffer;
import py.archive.ArchiveOptions;
import py.archive.page.PageAddress;
import py.exception.ChecksumMismatchedException;

public interface PageMetadata {
  final int MAGIC_NUMBER_OFFSET = 0;
  final int DATA_CHECKSUM_OFFSET = 8;
  final int VOLUMEID_OFFSET = 16;
  final int SEGMENT_INDEX_OFFSET = 24;
  final int SEGMENT_POSITION_OFFSET = 28;
  final int PAGE_POSITION_OFFSET = 36;
  final int METADATA_CHECKSUM_OFFSET = ArchiveOptions.PAGE_METADATA_LENGTH - 8;

  public PageAddress getAddress();

  public PageMetadata setAddress(PageAddress pageAddress);

  public ByteBuffer getBuffer(boolean needDuplicate);

  public ByteBuffer getDataBuffer();

  /**
   * update the meta data with new page address.
   */
  public void updateAddress(PageAddress pageAddress);

  public boolean isValid(PageAddress pageAddress);


  public int getPageSize();

  public int getPhysicalPageSize();

  public int getMetadataSize();

  public long getBufferPhysicalOffset(int offset);

  public int dataOffsetInPage(int offsetInData);
}
