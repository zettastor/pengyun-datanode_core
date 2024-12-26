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
