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
