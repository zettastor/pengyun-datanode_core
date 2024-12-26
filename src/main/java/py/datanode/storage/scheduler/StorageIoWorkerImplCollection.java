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

package py.datanode.storage.scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.datanode.configuration.DataNodeConfiguration;
import py.datanode.page.AvailablePageLister;
import py.datanode.page.DirtyPagePool;
import py.datanode.page.DirtyPagePoolFactory;
import py.storage.Storage;

public class StorageIoWorkerImplCollection
    implements StorageIoWorkerHouseKeeper, DirtyPagePoolFactory, AvailablePageLister {
  private static final Logger logger = LoggerFactory.getLogger(StorageIoWorkerImplCollection.class);

  private final Map<Storage, StorageIoWorkerImpl> storageStorageIoWorkerMap;
  private final DataNodeConfiguration cfg;

  public StorageIoWorkerImplCollection(DataNodeConfiguration cfg) {
    this.cfg = cfg;
    this.storageStorageIoWorkerMap = new ConcurrentHashMap<>();
  }

  private StorageIoWorkerImpl getOrBuild(Storage storage) {
    return storageStorageIoWorkerMap.computeIfAbsent(storage, s -> {
      StorageIoWorkerImpl storageIoWorkerImpl = new StorageIoWorkerImpl(
          Storage.getDeviceName(storage.identifier()), cfg);
      storageIoWorkerImpl.start();
      return storageIoWorkerImpl;
    });
  }

  public void removeWorker(Storage storage) {
    storageStorageIoWorkerMap.remove(storage);
  }

  public void stopAllAndClear() {
    for (StorageIoWorkerImpl storageIoWorker : storageStorageIoWorkerMap.values()) {
      storageIoWorker.stop();
    }
    storageStorageIoWorkerMap.clear();
  }

  public void removeUnusedWorkers(long lastIoTime) {
    List<Storage> storagesToBeRemoved = new ArrayList<Storage>();
    for (Map.Entry<Storage, StorageIoWorkerImpl> storageAndWorker : storageStorageIoWorkerMap
        .entrySet()) {
      Storage storage = storageAndWorker.getKey();
      StorageIoWorker tmpWorker = storageAndWorker.getValue();

      if (!(storage.isBroken())) {
        continue;
      }

      if (lastIoTime > tmpWorker.lastIoTime() && tmpWorker.pendingRequestCount() == 0) {
        logger.warn("remove the I/0 work: {}, storage: {} because it is idle", tmpWorker, storage);
        storagesToBeRemoved.add(storage);
      }
    }

    if (storagesToBeRemoved.size() > 0) {
      for (Storage tmpStorage : storagesToBeRemoved) {
        StorageIoWorker discardWorker = storageStorageIoWorkerMap.remove(tmpStorage);
        logger.warn("remove the worker:{} for storage: {}, left: {}", discardWorker, tmpStorage,
            storageStorageIoWorkerMap.size());
        if (discardWorker != null) {
          discardWorker.stop();
        }
      }
    }
  }

  @Override
  public void hasAvailblePage() {
    for (StorageIoWorkerImpl storageIoWorker : storageStorageIoWorkerMap.values()) {
      storageIoWorker.notifyPendingWork();
    }
  }

  @Override
  public void hasAvailblePage(Storage storage) {
    StorageIoWorkerImpl worker = storageStorageIoWorkerMap.get(storage);
    if (worker != null) {
      worker.notifyPendingWork();
    }
  }

  @Override
  public DirtyPagePool getOrBuildDirtyPagePool(Storage storage) {
    return getOrBuild(storage);
  }

  @Override
  public StorageIoWorker getOrBuildStorageIoWorker(Storage storage) {
    return getOrBuild(storage);
  }
}
