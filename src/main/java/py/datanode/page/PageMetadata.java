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
