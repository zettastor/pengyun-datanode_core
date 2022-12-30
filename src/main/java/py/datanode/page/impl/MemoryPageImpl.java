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

import com.google.common.collect.BoundType;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import java.nio.ByteBuffer;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.archive.page.PageAddress;
import py.common.DirectAlignedBufferAllocator;
import py.common.FastBuffer;
import py.datanode.page.PageMetadata;
import py.exception.ChecksumMismatchedException;

public class MemoryPageImpl extends AbstractPage {
  private static final Logger logger = LoggerFactory.getLogger(MemoryPageImpl.class);
  /**
   * the meta data of page, which is the id of the page.because containing page address.
   */
  private PageMetadata pageMetadata;

  public MemoryPageImpl(PageMetadata pageMetadata) {
    this.pageMetadata = pageMetadata;
  }

  public MemoryPageImpl(PageAddress pageAddress, ByteBuffer buffer) {
    this.pageMetadata = PageMetadataImpl.fromBuffer(buffer).setAddress(pageAddress);
  }

  public MemoryPageImpl(ByteBuffer buffer) {
    this(new BogusPageAddress(), buffer);
  }

  @Override
  public PageAddress getAddress() {
    return pageMetadata.getAddress();
  }

  @Override
  public void changeAddress(PageAddress pageAddress) {
    pageMetadata.updateAddress(pageAddress);
  }

  @Override
  public ByteBuffer getReadOnlyView() {
    return getReadOnlyView(0, pageMetadata.getPageSize());
  }

  /**
   * Returns a buffer that is read only and only contains the requested data.
   */
  @Override
  public ByteBuffer getReadOnlyView(int pageOffset, int length) {
    if (pageOffset + length > pageMetadata.getPageSize()) {
      logger.warn("view data at offset: {} length: {} data size in page: {} ", pageOffset, length,
          pageMetadata.getPageSize());
      Validate.isTrue(false);
    }

    ByteBuffer buffer = pageMetadata.getBuffer(false).asReadOnlyBuffer();
    int position = pageMetadata.dataOffsetInPage(pageOffset);
    buffer.position(position);
    buffer.limit(position + length);

    return buffer.slice();
  }

  @Override
  public void getData(int pageOffset, byte[] dst, int offset, int length) {
    if (pageOffset + length > pageMetadata.getPageSize()) {
      logger.warn("view data at offset: {} length: {} data size in page: {} ", pageOffset, length,
          pageMetadata.getPageSize());
      Validate.isTrue(false);
    }

    ByteBuffer buffer = pageMetadata.getBuffer(false);
    if (buffer.isDirect()) {
      DirectAlignedBufferAllocator
          .copyMemory(pageMetadata.getBufferPhysicalOffset(pageOffset), dst, offset, length);
    } else {
      Validate.isTrue(buffer.hasArray());
      System.arraycopy(buffer.array(),
          buffer.arrayOffset() + pageMetadata.dataOffsetInPage(pageOffset), dst,
          offset, length);
    }
  }

  public void getData(int pageOffset, ByteBuffer dstBuffer) {
    int length = dstBuffer.remaining();
    if (pageOffset + length > pageMetadata.getPageSize()) {
      logger.warn("view data at offset: {} length: {} data size in page: {} ", pageOffset, length,
          pageMetadata.getPageSize());
      Validate.isTrue(false);
    }

    if (dstBuffer.hasArray()) {
      getData(pageOffset, dstBuffer.array(), dstBuffer.arrayOffset() + dstBuffer.position(),
          length);
      return;
    }

    Validate.isTrue(dstBuffer.isDirect());
    long dstAddress = DirectAlignedBufferAllocator.getAddress(dstBuffer) + dstBuffer.position();
    ByteBuffer srcBuffer = pageMetadata.getBuffer(false);
    if (srcBuffer.hasArray()) {
      DirectAlignedBufferAllocator
          .copyMemory(srcBuffer.array(),
              srcBuffer.arrayOffset() + pageMetadata.dataOffsetInPage(pageOffset),
              length, dstAddress);
    } else {
      DirectAlignedBufferAllocator
          .copyMemory(pageMetadata.getBufferPhysicalOffset(pageOffset), dstAddress, length);
    }
  }

  @Override
  public void write(int offset, FastBuffer src) {
    throw new NotImplementedException("");
  }

  @Override
  public void write(int offsetInPage, FastBuffer src, RangeSet<Integer> srcDataRanges) {
    if (!getPageStatus().canModify()) {
      logger.error("can not modify the page: {}", this);
      Validate.isTrue(false);
    }

    if (srcDataRanges == null || srcDataRanges.isEmpty()) {
      throw new IllegalArgumentException(
          "range is empty. Can't apply to the page " + this.toString());
    }

    int length = (int) src.size();
    if (offsetInPage < 0 || offsetInPage + src.size() > pageMetadata.getPageSize()) {
      logger.error(
          "can't write data to the page because data can't fit in. offset={}, length of data={}," 
              + " buffer limit={}",
          offsetInPage, length, pageMetadata.getPageSize());
      Validate.isTrue(false);

    }

    ByteBuffer buffer = pageMetadata.getBuffer(false);
    for (Range<Integer> range : srcDataRanges.asRanges()) {
      int lowerEndPoint =
          range.lowerBoundType() == BoundType.CLOSED ? range.lowerEndpoint()
              : range.lowerEndpoint() + 1;
      int upperEndPoint =
          range.upperBoundType() == BoundType.CLOSED ? range.upperEndpoint()
              : range.upperEndpoint() - 1;
      int offsetInPageIncludingMetadata = pageMetadata
          .dataOffsetInPage(lowerEndPoint + offsetInPage);
      src.get(lowerEndPoint, buffer, offsetInPageIncludingMetadata,
          upperEndPoint - lowerEndPoint + 1);
    }
    setDirty(true);
  }

  @Override
  public void write(int offsetInPage, ByteBuffer src) {
    if (!getPageStatus().canModify()) {
      logger.error("can not modify the page: {}", this);
      Validate.isTrue(false);
    }

    int length = src.remaining();
    if (offsetInPage < 0 || offsetInPage + length > pageMetadata.getPageSize()) {
      String errMsg =
          "can't write data to the page because data can't fit in. offset: " + offsetInPage
              + " length of data: " + length + " buffer limit: " + pageMetadata.getPageSize();
      logger.warn(errMsg);
      Validate.isTrue(false);
    }

    ByteBuffer buffer = pageMetadata.getBuffer(false);
    if (buffer.isDirect()) {
      long srcAddress = DirectAlignedBufferAllocator.getAddress(src) + src.position();
      long desAddress = pageMetadata.getBufferPhysicalOffset(offsetInPage);
      if (src.isDirect()) {
        DirectAlignedBufferAllocator.copyMemory(srcAddress, desAddress, length);
      } else if (src.isReadOnly()) {
        DirectAlignedBufferAllocator.copyMemory(src, length, desAddress);
      } else {
       
        Validate.isTrue(src.hasArray());
        DirectAlignedBufferAllocator
            .copyMemory(src.array(), src.arrayOffset() + src.position(), length, desAddress);
      }
    } else {
      int realOffset = buffer.arrayOffset() + pageMetadata.dataOffsetInPage(offsetInPage);
      Validate.isTrue(buffer.hasArray());
      if (src.isDirect()) {
        long srcAddress = DirectAlignedBufferAllocator.getAddress(src) + src.position();
        DirectAlignedBufferAllocator.copyMemory(srcAddress, buffer.array(), realOffset, length);
      } else {
        if (src.hasArray()) {
          System.arraycopy(src.array(), src.arrayOffset() + src.position(), buffer.array(),
              realOffset,
              length);
        } else {
          buffer = pageMetadata.getBuffer(true);
          buffer.position(pageMetadata.dataOffsetInPage(offsetInPage));
          buffer.put(src);
        }
      }
    }

    setDirty(true);
  }

  @Override
  public boolean checkMetadata(PageAddress pageAddress) {
    if (pageMetadata.isValid(pageAddress)) {
      pageMetadata.setAddress(pageAddress);
      return true;
    }

    return false;
  }

  @Override
  public ByteBuffer getDataBuffer() {
    return pageMetadata.getDataBuffer();
  }

  public ByteBuffer getIoBuffer() {
    return pageMetadata.getBuffer(true);
  }

  @Override
  public int getPageSize() {
    return pageMetadata.getPageSize();
  }

  @Override
  public int getPhysicalPageSize() {
    return pageMetadata.getPhysicalPageSize();
  }


  @Override
  public String toString() {
    return "MemoryPageImpl [super=" + super.toString() + ", pageMetadata=" + pageMetadata + "]";
  }

}