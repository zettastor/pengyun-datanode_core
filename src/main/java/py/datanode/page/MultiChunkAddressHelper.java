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

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.archive.ArchiveOptions;
import py.archive.page.MultiPageAddress;
import py.archive.page.PageAddress;
import py.common.struct.Pair;
import py.datanode.page.impl.PageAddressGenerator;

public class MultiChunkAddressHelper {
  private static final Logger logger = LoggerFactory.getLogger(MultiChunkAddressHelper.class);

  static int chunkLength = 16;
  private final PageAddress startPageAddress;

  public MultiChunkAddressHelper(PageAddress startPageAddress) {
    this.startPageAddress = startPageAddress;
  }

  public static void initChunkLength(int chunkLength) {
    MultiChunkAddressHelper.chunkLength = chunkLength;
  }

  public static int getChunkLength() {
    return chunkLength;
  }

  public static int calculateChunkIndex(PageAddress pageAddress) {
    return PageAddressGenerator.calculatePageIndex(pageAddress) / chunkLength;
  }

  public static Pair<PageAddress, Integer> calculateStartPageAddress(
      MultiPageAddress multiPageAddress) {
    int pageIndex = PageAddressGenerator.calculatePageIndex(multiPageAddress.getStartPageAddress());

    int startPageIndex = (pageIndex / chunkLength) * chunkLength;
    PageAddress startPageAddress = PageAddressGenerator.generate(
        multiPageAddress.getStartPageAddress().getSegId(),
        multiPageAddress.getStartPageAddress().getSegUnitOffsetInArchive(), startPageIndex,
        multiPageAddress.getStartPageAddress().getStorage(),
        ArchiveOptions.PAGE_SIZE
    );

    logger.debug("calculate start page address for {}, page index {}, result {}", multiPageAddress,
        pageIndex, startPageAddress);
    return new Pair<>(startPageAddress, pageIndex - startPageIndex);
  }

  public static MultiPageAddress getChunkAddressFromChildAddress(PageAddress childAddress) {
    int chunkIndex = calculateChunkIndex(childAddress);
    int startPageIndex = chunkIndex * chunkLength;
    PageAddress startPageAddress = PageAddressGenerator
        .generate(childAddress.getSegId(), childAddress.getSegUnitOffsetInArchive(), startPageIndex,
            childAddress.getStorage(), ArchiveOptions.PAGE_SIZE);
    return new MultiPageAddress(startPageAddress, chunkLength);

  }

  public static List<MultiPageAddress> splitMultiPageAddressByChunk(
      MultiPageAddress multiPageAddress) {
    List<MultiPageAddress> splitedMultiPageAddress = new ArrayList<>();
    int pageIndex = PageAddressGenerator.calculatePageIndex(multiPageAddress.getStartPageAddress());

    int startPageIndex =
        (pageIndex / MultiChunkAddressHelper.getChunkLength()) * MultiChunkAddressHelper
            .getChunkLength();
    int holdPages = MultiChunkAddressHelper.getChunkLength() - (pageIndex - startPageIndex);
    holdPages =
        holdPages > multiPageAddress.getPageCount() ? multiPageAddress.getPageCount() : holdPages;

    MultiPageAddress originPageAddress = new MultiPageAddress(
        multiPageAddress.getStartPageAddress(), holdPages);
    splitedMultiPageAddress.add(originPageAddress);

    int pageCount = multiPageAddress.getPageCount() - holdPages;
    while (pageCount > 0) {
      PageAddress originStartPageAddress = PageAddressGenerator.generate(
          originPageAddress.getStartPageAddress().getSegId(),
          originPageAddress.getStartPageAddress().getSegUnitOffsetInArchive(),
          multiPageAddress.getStartPageAddress().getOffsetInSegment()
              + (holdPages) * ArchiveOptions.PAGE_PHYSICAL_SIZE,
          multiPageAddress.getStartPageAddress().getStorage());

      holdPages =
          pageCount > MultiChunkAddressHelper.getChunkLength() ? MultiChunkAddressHelper
              .getChunkLength()
              : pageCount;

      originPageAddress = new MultiPageAddress(originStartPageAddress, holdPages);
      splitedMultiPageAddress.add(originPageAddress);

      pageCount -= holdPages;
      pageIndex += holdPages;
    }

    if (splitedMultiPageAddress.size() > 1) {
      logger
          .debug("split multi page address by chunk, input address {}, result {}", multiPageAddress,
              splitedMultiPageAddress);
    }
    return splitedMultiPageAddress;
  }

  public PageAddress getStartPageAddress() {
    return startPageAddress;
  }

  @Override
  public String toString() {
    return "MultiChunkAddressHelper{" 
        + "startPageAddress=" + startPageAddress 
        + '}';
  }
}
