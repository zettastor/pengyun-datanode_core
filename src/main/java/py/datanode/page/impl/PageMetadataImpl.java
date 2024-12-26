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

package py.datanode.page.impl;

import java.nio.ByteBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.archive.ArchiveOptions;
import py.archive.page.PageAddress;
import py.archive.page.PageAddressImpl;
import py.archive.segment.SegId;
import py.common.DirectAlignedBufferAllocator;
import py.datanode.page.PageMetadata;
import py.storage.Storage;

public class PageMetadataImpl implements PageMetadata {
  private static final Logger logger = LoggerFactory.getLogger(PageMetadataImpl.class);

  private final ByteBuffer buffer;
  private PageAddress pageAddress;

  private PageMetadataImpl(ByteBuffer byteBuffer) {
    this.buffer = byteBuffer;
  }

  /**
   * build a metadata instance.
   */
  public static PageMetadata fromBuffer(ByteBuffer buffer) {
    return new PageMetadataImpl(buffer);
  }

  /**
   * recovery the metadata from buffer.
   */
  public static PageMetadata fromBuffer(ByteBuffer buffer, Storage storage) {
    if (!validateMetadata(buffer)) {
      return null;
    }

    PageAddress address = getAddress(buffer, storage);
    if (BogusPageAddress.isAddressBogus(address)) {
      return new PageMetadataImpl(buffer).setAddress(new BogusPageAddress());
    } else {
      return new PageMetadataImpl(buffer).setAddress(address);
    }
  }

  private static ByteBuffer getMetadataBuffer(ByteBuffer buffer) {
    ByteBuffer byteBuffer = buffer.duplicate();
    byteBuffer.position(0);
    byteBuffer.limit(METADATA_CHECKSUM_OFFSET);
    return byteBuffer;
  }

  @Override
  public PageAddress getAddress() {
    return pageAddress;
  }
  
  
  public static PageAddress getAddress(ByteBuffer buffer, Storage storage) {
    long volumeId = buffer.getLong(VOLUMEID_OFFSET);
    int segmentIndex = buffer.getInt(SEGMENT_INDEX_OFFSET);
    long segmentOffsetInArchive = buffer.getLong(SEGMENT_POSITION_OFFSET);
    long pageOffsetInSegment = buffer.getLong(PAGE_POSITION_OFFSET);
   
    logger.debug(
        "get address volumeId {}, segmentIndex {}, segmentOffsetInArchive {}, " 
            + "pageOffsetInSegment {}, storage: {}",
        volumeId, segmentIndex, segmentOffsetInArchive, pageOffsetInSegment, storage);
    return new PageAddressImpl(new SegId(volumeId, segmentIndex), segmentOffsetInArchive,
        pageOffsetInSegment,
        storage);
  }

  private static boolean validatePageAddress(PageAddress srcAddress, ByteBuffer buffer) {
    long volumeId = buffer.getLong(VOLUMEID_OFFSET);
    int segmentIndex = buffer.getInt(SEGMENT_INDEX_OFFSET);
    long segmentOffsetInArchive = buffer.getLong(SEGMENT_POSITION_OFFSET);
    long pageOffsetInSegment = buffer.getLong(PAGE_POSITION_OFFSET);

    if (segmentOffsetInArchive != srcAddress.getSegUnitOffsetInArchive()) {
      return false;
    }

    if (pageOffsetInSegment != srcAddress.getOffsetInSegment()) {
      return false;
    }

    return true;
  }

  private static boolean validateMetadata(ByteBuffer buffer) {
    if (buffer.getLong(MAGIC_NUMBER_OFFSET) != ArchiveOptions.PAGE_MAGIC_NUM) {
      return false;
    }
    return true;
  }

  @Override
  public ByteBuffer getBuffer(boolean needDuplicate) {
    if (needDuplicate) {
      return buffer.duplicate();
    } else {
      return buffer;
    }
  }

  @Override
  public ByteBuffer getDataBuffer() {
    ByteBuffer byteBuffer = buffer.duplicate();
    byteBuffer.position(dataOffsetInPage(0));
    return byteBuffer;
  }

  @Override
  public int getPageSize() {
    return buffer.capacity() - ArchiveOptions.PAGE_METADATA_LENGTH;
  }

  @Override
  public int getPhysicalPageSize() {
    return buffer.capacity();
  }

  public int getMetadataSize() {
    return ArchiveOptions.PAGE_METADATA_LENGTH;
  }

  @Override
  public long getBufferPhysicalOffset(int offset) {
    return DirectAlignedBufferAllocator.getAddress(buffer) + dataOffsetInPage(offset);
  }

  @Override
  public void updateAddress(PageAddress pageAddress) {
    this.pageAddress = pageAddress;
    buffer.putLong(VOLUMEID_OFFSET, pageAddress.getSegId().getVolumeId().getId());
    buffer.putInt(SEGMENT_INDEX_OFFSET, pageAddress.getSegId().getIndex());
    buffer.putLong(SEGMENT_POSITION_OFFSET, pageAddress.getSegUnitOffsetInArchive());
    buffer.putLong(PAGE_POSITION_OFFSET, pageAddress.getOffsetInSegment());
  }

  @Override
  public PageMetadata setAddress(PageAddress pageAddress) {
    this.pageAddress = pageAddress;
    return this;
  }

  @Override
  public boolean isValid(PageAddress pageAddress) {
    return validateMetadata(buffer) && validatePageAddress(pageAddress, buffer);
  }

  @Override
  public int dataOffsetInPage(int offsetInData) {
    return ArchiveOptions.PAGE_METADATA_LENGTH + offsetInData;
  }

  @Override
  public String toString() {
    return "PageMetadataImpl [pageAddress=" + pageAddress + "]";
  }

}