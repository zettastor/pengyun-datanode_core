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

import java.nio.ByteBuffer;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.archive.ArchiveOptions;
import py.archive.page.PageAddress;
import py.archive.page.PageAddressImpl;
import py.archive.segment.SegId;
import py.exception.StorageException;
import py.storage.Storage;

/**
 * The factory class to generate a PageAddress instance.
 */
public class PageAddressGenerator {
  private static final Logger logger = LoggerFactory.getLogger(PageAddressGenerator.class);

  public static PageAddress generate(SegId segId, long segUnitPhysicalAddrInArchive,
      long dataLogicalOffsetInSegUnit,
      Storage storage, long pageSize) {
    return generate(segId, segUnitPhysicalAddrInArchive, dataLogicalOffsetInSegUnit, storage,
        pageSize,
        ArchiveOptions.PAGE_METADATA_NEED_FLUSH_DISK);
  }

  public static PageAddress generate(SegId segId, long segUnitPhysicalAddrInArchive,
      long dataLogicalOffsetInSegUnit,
      Storage storage, long pageSize, boolean havingPageMetadata) {
    long pageOffsetInSegUnit = calculatePageOffsetInSegUnit(dataLogicalOffsetInSegUnit, pageSize);
    if (havingPageMetadata) {
      pageOffsetInSegUnit += pageOffsetInSegUnit / pageSize * ArchiveOptions.PAGE_METADATA_LENGTH;
    }
    return new PageAddressImpl(segId, segUnitPhysicalAddrInArchive, pageOffsetInSegUnit, storage);
  }

  public static PageAddress generate(SegId segId, long segUnitPhysicalAddressInArchive,
      long dataPhysicalOffsetInSegUnit, Storage storage) {
    return new PageAddressImpl(segId, segUnitPhysicalAddressInArchive, dataPhysicalOffsetInSegUnit,
        storage);
  }

  public static PageAddress generate(SegId segId, long segUnitPhysicalAddrInArchive, int pageIndex,
      Storage storage,
      long pageSize) {
    return generate(segId, segUnitPhysicalAddrInArchive, pageIndex, storage, pageSize,
        ArchiveOptions.PAGE_METADATA_NEED_FLUSH_DISK);
  }

  public static PageAddress generate(SegId segId, long segUnitPhysicalAddrInArchive, int pageIndex,
      Storage storage,
      long pageSize, boolean havingPageMetadata) {
    long pageOffsetInSegUnit = (long) pageIndex
        * (pageSize + (havingPageMetadata ? ArchiveOptions.PAGE_METADATA_LENGTH : 0L));
    return new PageAddressImpl(segId, segUnitPhysicalAddrInArchive, pageOffsetInSegUnit, storage);
  }

  public static PageAddress generate(PageAddress originPage, int incrementPage) {
    long dataPhysicalOffsetInSegUnit =
        originPage.getOffsetInSegment() + (incrementPage * ArchiveOptions.PAGE_PHYSICAL_SIZE);
    Validate.isTrue(dataPhysicalOffsetInSegUnit < ArchiveOptions.SEGMENT_PHYSICAL_SIZE);
    return generate(originPage.getSegId(), originPage.getSegUnitOffsetInArchive(),
        dataPhysicalOffsetInSegUnit, originPage.getStorage());
  }

  public static PageAddress generateAddressOfShadowPage(long physicalOffsetInArchive,
      long segmentUnitDataStartPosition, Storage storage) {
    logger.debug(
        "physicalOffsetInArchive is {}, segmentUnitDataStartPosition is {}, " 
            + "PAGE_PHYSICAL_SIZE is {}",
        physicalOffsetInArchive, segmentUnitDataStartPosition, ArchiveOptions.PAGE_PHYSICAL_SIZE);
    if (py.datanode.page.impl.GarbagePageAddress.isGarbageOffset(physicalOffsetInArchive)) {
      return new py.datanode.page.impl.GarbagePageAddress();
    }

    Validate.isTrue(
        (physicalOffsetInArchive - segmentUnitDataStartPosition) % ArchiveOptions.PAGE_PHYSICAL_SIZE
            == 0);
    SegId segId = SegId.SYS_RESERVED_SEGID;
    long index = (physicalOffsetInArchive - segmentUnitDataStartPosition)
        / ArchiveOptions.SEGMENT_PHYSICAL_SIZE;
    long segUnitPhysicalAddrInArchive =
        segmentUnitDataStartPosition + index * ArchiveOptions.SEGMENT_PHYSICAL_SIZE;
    long pageOffsetInSegUnit = (physicalOffsetInArchive - segmentUnitDataStartPosition)
        % ArchiveOptions.SEGMENT_PHYSICAL_SIZE;
   
    return new PageAddressImpl(segId, segUnitPhysicalAddrInArchive, pageOffsetInSegUnit, storage);
  }

  public static int calculatePageIndex(PageAddress pageAddress) {
    return (int) (pageAddress.getOffsetInSegment() / ArchiveOptions.PAGE_PHYSICAL_SIZE);
  }
  
  public static int calculatePageIndex(long dataLogicalOffsetInSegUnit, long pageSize) {
    return (int) (dataLogicalOffsetInSegUnit / pageSize);
  }

  public static long calculatePageOffsetInSegUnit(long dataLogicalOffsetInSegUnit, long pageSize) {
    return (dataLogicalOffsetInSegUnit / ((long) pageSize)) * ((long) pageSize);
  }

  public static long calculateDataPhysicalOffsetInSegUnit(int pageIndex) {
    return pageIndex * ArchiveOptions.PAGE_PHYSICAL_SIZE;
  }

  public static boolean isConnected(PageAddress p1, PageAddress p2) {
    return p1.getSegId().equals(p2.getSegId())
        && p2.getOffsetInSegment() - p1.getOffsetInSegment() 
        == ArchiveOptions.PAGE_PHYSICAL_SIZE;
  }

  public static boolean isConnected(long offset1, long offset2) {
    return offset2 - offset1 == ArchiveOptions.PAGE_PHYSICAL_SIZE;
  }

  public static PageAddress generateMaxPageAddress() {
    return new PageAddressImpl(new SegId(Long.MAX_VALUE, Integer.MAX_VALUE), 0, Long.MAX_VALUE,
        new Storage("generate max value") {
          @Override
          public void read(long pos, ByteBuffer buffer) throws StorageException {
            throw new NotImplementedException("generate for exit");
          }
          
          @Override
          public void read(long pos, byte[] dstBuf, int off, int len) throws StorageException {
            throw new NotImplementedException("generate for exit");
          }

          @Override
          public void write(long pos, byte[] buf, int off, int len) throws StorageException {
            throw new NotImplementedException("generate for exit");
          }

          @Override
          public void write(long pos, ByteBuffer buffer) throws StorageException {
            throw new NotImplementedException("generate for exit");
          }

          @Override
          public long size() {
            throw new NotImplementedException("generate for exit");
          }

          @Override
          public long getId() {
            return Long.MAX_VALUE;
          }
        });
  }
}
