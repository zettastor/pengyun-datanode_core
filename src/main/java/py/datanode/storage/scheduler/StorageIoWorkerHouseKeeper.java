
package py.datanode.storage.scheduler;

import py.storage.Storage;

public interface StorageIoWorkerHouseKeeper {
  StorageIoWorker getOrBuildStorageIoWorker(Storage storage);

  void removeWorker(Storage storage);

  void stopAllAndClear();

  void removeUnusedWorkers(long lastIoTime);
}
