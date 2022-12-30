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
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.archive.ArchiveOptions;
import py.archive.page.PageAddress;
import py.common.DirectAlignedBufferAllocator;
import py.datanode.configuration.DataNodeConfiguration;
import py.datanode.page.Page;

public class MemoryPageManagerFactory implements PageManagerFactory {
  private static final Logger logger = LoggerFactory.getLogger(MemoryPageManagerFactory.class);
  private static final int MIN_PAGE_TIEMS_OF_PAGE_CACHE = 4;

  private final DataNodeConfiguration cfg;
  private final StorageIoDispatcher storageIoDispatcher;

  public MemoryPageManagerFactory(DataNodeConfiguration cfg,

      StorageIoDispatcher storageIoDispatcher) {
    this.cfg = cfg;
    this.storageIoDispatcher = storageIoDispatcher;
  }

  /**
   * construct page list for Memory.
   */
  public PageManagerImpl build(long memoryCacheSize, String name) {
    long numPages = memoryCacheSize / ArchiveOptions.PAGE_SIZE_IN_PAGE_MANAGER;

    MemoryPageFactory<Page> pageFactory = (buffer, pageAddress) -> new MemoryPageImpl(pageAddress,
        buffer);

    logger.info("enable the archive config={}, pageCount: {}", cfg.getArchiveConfiguration(),
        numPages);
    List<Page> pages = makeMemoryMappedPages(pageFactory, numPages);

    PageManagerImpl pageManager = new PageManagerImpl(pages, cfg, name,
        storageIoDispatcher);
    long startTime = System.currentTimeMillis();
    logger.warn("end build ssd storage, cost time: {}s",
        (System.currentTimeMillis() - startTime) / 1000);

    return pageManager;
  }

  /**
   * creates the pages from a memory mapped file.
   */
  private List<Page> makeMemoryMappedPages(MemoryPageFactory<Page> factory, long numPages) {
    if (numPages < MIN_PAGE_TIEMS_OF_PAGE_CACHE) {
      logger.warn("the count of page in page system is too little, page count: " + numPages);
    }

    logger.info("memory pages: {}, page size: {}", numPages,
        ArchiveOptions.PAGE_SIZE_IN_PAGE_MANAGER);
    long pageSize = ArchiveOptions.PAGE_SIZE_IN_PAGE_MANAGER;

    long pageCountForMaxInteger = Integer.MAX_VALUE / pageSize - 1;
    ByteBuffer currentBuffer = null;
    ArrayList<Page> pages = new ArrayList<Page>();
    int position = 0;

    for (long i = 0; i < numPages; i++) {
      if (i % pageCountForMaxInteger == 0) {
        long mapSize =
            (numPages - i) > pageCountForMaxInteger ? pageCountForMaxInteger : (numPages - i);
        currentBuffer = DirectAlignedBufferAllocator
            .allocateAlignedByteBuffer((int) (mapSize * pageSize));
        position = 0;
      } else {
        position += pageSize;
      }

      currentBuffer.position(position);
      currentBuffer.limit(position + (int) pageSize);
      ByteBuffer buffer = currentBuffer.slice();

      buffer.clear();
      pages.add(factory.build(buffer, new BogusPageAddress()));
    }

    return pages;
  }

  private interface MemoryPageFactory<T> {
    T build(ByteBuffer bytes, PageAddress pageAddress);
  }
}
