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

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.archive.ArchiveOptions;
import py.archive.page.PageAddress;
import py.datanode.configuration.DataNodeConfiguration;
import py.datanode.page.PageManager;
import py.datanode.storage.scheduler.StorageIoWorkerImplCollection;

public class WrappedPageManagerFactory implements PageManagerFactory {
  private static final Logger logger = LoggerFactory.getLogger(WrappedPageManagerImpl.class);

  private final DataNodeConfiguration dataNodeCfg;
  private final StorageIoDispatcher storageIoDispatcher;
  private PageManagerDispatcher pageManagerDispatcher;

  public WrappedPageManagerFactory(DataNodeConfiguration dataNodeCfg,
      StorageIoDispatcher storageIoDispatcher,
      PageManagerDispatcher pageManagerDispatcher) {
    this.dataNodeCfg = dataNodeCfg;
    this.storageIoDispatcher = storageIoDispatcher;
    this.pageManagerDispatcher = pageManagerDispatcher;
  }

  public WrappedPageManagerFactory(DataNodeConfiguration dataNodeCfg,
      StorageIoDispatcher storageIoDispatcher) {
    this(dataNodeCfg, storageIoDispatcher,
        SegIndexHashedPageManagerDispatcher.INSTANCE);
  }

  @Override
  public WrappedPageManagerImpl build(long memoryCacheSize, String name) {
    if (memoryCacheSize <= 0) {
      memoryCacheSize = PageSystemMemoryAllocator.getPageSystemMemoryCacheSize();
      logger
          .warn("has not configured page system cache size, specify the size {}", memoryCacheSize);
    } else {
      logger.warn("has configured page system cache size, specify the size {}", memoryCacheSize);
    }

    MemoryPageManagerFactory memoryPageManagerFactory = new MemoryPageManagerFactory(dataNodeCfg,
        storageIoDispatcher);

    int pageSystemCount = dataNodeCfg.getPageSystemCount();
    if (pageSystemCount <= 0) {
      pageSystemCount = Runtime.getRuntime().availableProcessors();
      logger.warn(
          "has not configured page system count, set to the count of available processors : {}",
          pageSystemCount);
    } else {
      logger.warn("has configured page system count {}", pageSystemCount);
    }

    List<PageManagerImpl> pageManagers = new ArrayList<>(pageSystemCount);
    for (int i = 0; i < pageSystemCount; i++) {
      pageManagers
          .add(memoryPageManagerFactory.build(memoryCacheSize / pageSystemCount, name + "_" + i));
    }

    StorageIoWorkerImplCollection storageIoWorkerImplCollection = new StorageIoWorkerImplCollection(
        dataNodeCfg);
    WrappedPageManagerImpl wrappedPageManagerImpl = new WrappedPageManagerImpl(dataNodeCfg,
        storageIoWorkerImplCollection, storageIoWorkerImplCollection, storageIoWorkerImplCollection,
        storageIoDispatcher, pageManagerDispatcher, pageManagers);
    wrappedPageManagerImpl.start();
    return wrappedPageManagerImpl;
  }

  public enum SegIndexHashedPageManagerDispatcher implements PageManagerDispatcher {
    INSTANCE;

    @Override
    public <P extends PageManager> P select(List<P> candidates, PageAddress address) {
      long segmentIndexOnArchive =
          address.getSegUnitOffsetInArchive() / ArchiveOptions.SEGMENT_PHYSICAL_SIZE;
      return candidates.get((int) (segmentIndexOnArchive % candidates.size()));
    }
  }

}
