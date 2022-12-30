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

package py.datanode.test;

import java.io.File;
import py.archive.ArchiveOptions;
import py.datanode.archive.ArchiveIdFileRecorder;
import py.datanode.configuration.ArchiveConfiguration;
import py.datanode.configuration.DataNodeConfiguration;
import py.datanode.configuration.LogPersistRocksDbConfiguration;
import py.datanode.configuration.RocksDbPathConfig;
import py.storage.EdRootpathSingleton;
import py.storage.StorageConfiguration;

public class DataNodeConfigurationForTest extends DataNodeConfiguration {
  boolean useLocalhost = true;

  public DataNodeConfigurationForTest() {
    super();
    StorageConfiguration storageConfiguration = new StorageConfiguration();
   
    storageConfiguration.setSegmentSizeByte(16 * 1024 * 1024);
    storageConfiguration.setIoTimeoutMs(120 * 1000);
    EdRootpathSingleton.getInstance().setRootPath("/tmp/opt/scan");

    ArchiveConfiguration archiveConfiguration = new ArchiveConfiguration();
    archiveConfiguration.setPersistRootDir("/tmp");
    archiveConfiguration.setSnapshotLogDir("/tmp/snapshot");

    this.setRocksDbPartitionFolder("rocksdb");
    this.setRocksDbDaultFolder("default");
    File defaultDir;
    defaultDir = new File("/tmp/rocksdb/default");
    defaultDir.mkdirs();
    this.setSsdCacheRocksDbPath("/tmp/rocksdb/default");

    this.setArchiveConfiguration(archiveConfiguration);
    this.setArchiveReservedRatio(0);
    this.setStorageConfiguration(storageConfiguration);
    this.setIndexerLogRoot("/tmp/snapshotIndexer");
    this.setIndexerMaxTransactions(1000);
    this.setIndexerMaxFlushers(5);
    this.setIndexerFlusherBundleSize(1);
    this.setIndexerFlusherBundleTimeoutSec(2L);
    this.setIndexerMaxSizePerLog(1024 * 1024);
    this.setIndexerMaxOpenedLogFiles(10);
    this.setEnableIndexerStrictlySyncLog(false);
    this.setIndexerMaxEntriesPerDeletion(100);
    this.setIndexerMaxLazyFinalizations(1);
    this.setIndexerRetryIntervalSecAfterStorageException(0);
    this.setIndexerRetryTimesAfterStorageException(3);
    this.setStartFlusher(true);
    this.setMaxSynchronizeTimeForCreateLogMs(5000);
    this.setWaitForMemoryBufferTimeoutMs(5000);
    this.setPageSystemMemoryCacheSize("10M");
   
    this.setThresholdToRequestForNewMemberMs(30 * 1000);
    this.setNumberOfCopyPagePerStorage(10);
    this.setLogDirs("/tmp");
    this.setArbiterCountLimitInOneArchive(500);
    this.setFlexibleCountLimitInOneArchive(500);
    this.setArbiterCountLimitInOneDatanode(15000);
    this.setFlexibleCountLimitInOneArchive(64);
    this.setIndexerDbPath("indexer");
    this.setIndexerDeletionBatchLimit(100000);
    this.setIndexerDeletionDelaySec(600);
    this.setRateOfJanitorExecutionMs(10000);
    this.setRateOfDuplicatingVolumeMetadataJsonMs(10000);
    this.setRocksDbPathConfig(new RocksDbPathConfig(this, new LogPersistRocksDbConfiguration()));
    this.setArbiterStoreFile("/tmp/arbiter.data");
    this.setRateOfReportingSegmentsMs(1000);
    this.setArchiveIdRecordPath("/tmp/archiveIdRecord/");
    this.setStartSlowDiskChecker(false);
    this.setSecondaryFirstVotingTimeoutMs(3000);
    this.setIndexerEnableTracing(true);

    this.setBrickPreAllocatorLowerThreshold(0);
    this.setBrickPreAllocatorUpperThreshold(0);
    this.setNeedRetryBroadCastWhenBecomePrimary(false);
    ArchiveOptions.initContants((int) storageConfiguration.getPageSizeByte(),
        storageConfiguration.getSegmentSizeByte(), ArchiveOptions.DEFAULT_MAX_FLEXIBLE_COUNT);

    ArchiveIdFileRecorder.IMPROPERLY_EJECTED_RAW.init(this.getArchiveIdRecordPath());

    File logFilesRoot = new File(this.getIndexerLogRoot());
    if (logFilesRoot.exists()) {
      for (File file : logFilesRoot.listFiles()) {
        file.delete();
      }
    } else {
      logFilesRoot.mkdir();
    }
  }

  public boolean isUseLocalhost() {
    return useLocalhost;
  }

  public void setUseLocalhost(boolean useLocalhost) {
    this.useLocalhost = useLocalhost;
  }
}
