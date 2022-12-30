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

package py.datanode.configuration;

import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import org.apache.commons.lang.StringUtils;
import org.rocksdb.CompressionType;
import org.rocksdb.InfoLogLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import py.app.NettyConfiguration;
import py.archive.ArchiveOptions;
import py.datanode.archive.ArchiveIdFileRecorder;
import py.informationcenter.Utils;
import py.storage.StorageConfiguration;

@Configuration
@PropertySource("classpath:config/datanode.properties")
@Import({StorageConfiguration.class, ArchiveConfiguration.class,
    LogPersistRocksDbConfiguration.class, NettyConfiguration.class})
public class DataNodeConfiguration {
  private static Logger logger = LoggerFactory.getLogger(DataNodeConfiguration.class);
  // the segment unit size and page size are included in StorageConfiguration
  @Autowired
  private StorageConfiguration storageConfiguration = new StorageConfiguration();
  @Autowired
  private NettyConfiguration nettyConfiguration = new NettyConfiguration();
  @Value("${enable.page.address.validation:true}")
  private boolean enablePageAddressValidation = true;
  @Value("${rocksdb.in.SSD:true}")
  private boolean rocksDbInSsd = true;
  @Value("${rocksdb.default.folder:default}")
  private String rocksDbDaultFolder = "default";
  @Value("${rocksdb.partition.folder:partition}")
  private String rocksDbPartitionFolder = "partition";
  // arbiter segment meta data be saved into
  @Value("${arbiter.store.file:var/storage/arbiter.data}")
  private String arbiterStoreFile = "var/storage/arbiter.data";
  @Autowired
  private ArchiveConfiguration archiveConfiguration = new ArchiveConfiguration();
  // how many storage exceptions do we tolerate before treating a disk as bad
  @Value("${degrade.disk.storage.threshold:2000000}")
  private int degradeDiskStorageExceptionThreshold = 2000000;
  // how many storage exceptions do we tolerate before treating a disk as bad
  @Value("${broken.disk.storage.threshold:20000000}")
  private int brokenDiskStorageExceptionThreshold = 20000000;
  /************************* configurations for page manager. *************************/
  // the value can be calculated, because we know the ram disk size,
  // and the number of swapping page,
  private int numberOfPages = 0;
  @Value("${infinite.timeout.allocating.fast.buffer:true}")
  private boolean infiniteTimeoutAllocatingFastBuffer = true;
  @Value("${start.slow.disk.checker:true}")
  private boolean startSlowDiskChecker = true;

  @Value("${memory.size.for.data.logs.mb.per.archive:200}")
  private int memorySizeForDataLogsMb = 200;

  // if set true, the page which is dirty won't be flushed to disk in background,
  // you must force to flush the dirty
  // pages. it is mainly for junit test.
  private boolean startFlusher = true;

  // Default timeout for page requests if the caller didn't specify one
  // 20 seconds (it is 5 seconds before, but we found it too short
  @Value("${page.request.timeout.ms:20000}")
  private int defaultPageRequestTimeoutMs = 20000;
  // --tyr)

  @Value("${page.flush.interval.ms:1000}")
  private int flushPagesIntervalMs = 1000;

  @Value("${number.of.page.correctors:4}")
  private int numberOfPageCorrectors = 4;

  /**************************** Primary lease time out. **************************************/
  @Value("${secondary.lease.in.primary.ms:10000}")
  private int secondaryLeaseInPrimaryMs = 10000;

  // make sure the secondary lease is longer enough than secondaryLeaseInPrimaryMS so that when
  // the primary lease always expires prior to secondaries
  @Value("${secondary.lease.ms:14000}")
  private int secondaryLeaseMs = 14000;

  // 30 seconds
  @Value("${new.born.secondary.lease.ms:30000}")
  private int newBornSecondaryLeaseMs = 30000;

  // make sure the timeout is less than secondaryLeaseInPrimaryMS
  // so that the primary still can check its
  // lease expiration
  @Value("${primary.extend.lease.request.timeout.ms:2000}")
  private int extensionRequestTimeOutMs = 2000;

  // the connection timeout of host heartbeat
  @Value("${primary.extend.lease.connection.timeout.ms:2000}")
  private int extensionConnectionTimeoutMs = 2000; // 2 seconds

  // the expiration time for other status other than primary and secondary,
  // make it long enough so as to make the
  // voting process stable.
  @Value("${lease.for.other.statuses.ms:20000}")
  private int otherStatusesLeaseMs = 20000; // 20 seconds

  /**************************** Page manager. ************************************************/

  // when the page manager is being closed, how fast should we poll to see if all pages
  // have been released
  private long inteverlForCheckPageReleasedMs = 100; // 100 ms

  /*************************** Archive Manager options. *************************************/
  // the rate of reporting archives to the information center
  // doesn't need to be very fast at all.
  @Value("${archive.report.rate.ms:120000}")
  private int rateOfReportingArchivesMs = 120000; // 2 minute

  // We only use 95% of the archive space, the rest 5 percentage is
  // reserved for internal usage
  @Value("${archive.reserved.ratio:0.05}")
  private double archiveReservedRatio = 0.05;

  @Value("${archive.used.space.calculate.delay.ms:30000}") //30s
  private int archiveUsedSpaceCalculateDelayMs = 30000;
  /*************************** Segment Manager options. *************************************/
  // the rate of reporting segment units to the information center
  // same as secondaryLeaseInPrimaryMs so that when a secondary or primary timed out,
  // they can report its status very fast.
  @Value("${segment.report.rate.ms:60000}")
  private int rateOfReportingSegmentsMs = 60000; // same as secondaryLeaseInPrimaryMS;

  // the timeout of create segment units to return
  // the create segment units response to the control center
  @Value("${segment.create.timeout.ms:60000}")
  private int timeoutOfCreateSegmentsMs = 20000;

  /**************************** Request time out between data node. ***************************/
  // make it larger than or equal to the primary lease length so that
  // when the primary got timed out, the primary has
  // to lose it primary ship. In other words,
  // the primary can't get responses from the majority of secondaries.
  @Value("${datanode.request.timeout.ms:10000}")
  private int dataNodeRequestTimeoutMs = 10000; // 10 seconds

  /******************************** Ticket processor options. *********************************/
  // how long should a new segment unit request be sent since the last request.
  // this is the configuration to throttle too many failed requests. In other words, this
  // configuration is an time interval between two failed requests.
  @Value("${request.new.segment.unit.expiration.threshold.ms:120000}")
  private long requestNewSegmentUnitExpirationThresholdMs = 120000; // 2 minutes for Janitor

  /******************************** Rate of executing thing. ************************************/
  // the rate of executing lease extension from the primary
  @Value("${lease.extend.rate.ms:1500}")
  private int rateOfExtendingLeaseMs = 1500;

  // the rate of executing
  @Value("${segmentunit.deleter.delay.rate.ms:600000}")
  private int rateOfSegmentUnitDeleterDalayMs = 5000; // 5s

  // the rate of executing segmentUnitDeleter
  @Value("${segmentunitdeleter.execution.rate.ms:600000}")
  private int rateOfSegmentUnitDeleterExecutionMs = 600000; // 10 minutes

  /*** the interval for a segment unit to move from Deleting to Deleted status. *****/
  @Value("${wait.time.ms.to.move.segment.to.deleted:86400000}")
  private long waitTimeMsToMoveToDeleted = 86400000; // 24 hours

  // just move broken segment units to broken immediately
  /*** the interval for a segment unit to move from broken to deleted. ****/
  @Value("${wait.time.ms.from.broken.to.deleted:0}")
  private long waitTimeMsFromBrokenToDeleted = 0;

  @Value("${need.check.rollback.in.offling:true}")
  private boolean needCheckRollbackInOffling = true;

  /**
   * there is a quarantine zone before segment status change from deleting to deleted. In the
   * quarantine zone, volume recycling is not allowed. For example : <br> The
   * {wait.time.ms.to.move.segment.to.deleted} value is 120 seconds and <br> the
   * {quarantine.zone.ms.to.create.deleted.segment} value is 60 seconds.<br> So there are 120s
   * seconds for a deleted volume to be totally removed, and we can only recycle that volume in the
   * first 60 seconds.<br>
   *
   * <p>ATTENTION : keep this value the same as segment unit report rate
   */
  @Value("${quarantine.zone.ms.to.create.deleted.segment:60000}")
  private long quarantineZoneMsToRecoverDeletingSegment = 60000; // 60 seconds

  @Value("${duplicate.volume.metadata.json.rate.ms:60000}")
  private long rateOfDuplicatingVolumeMetadataJsonMs = 60000; // 1 minute

  @Value("${janitor.execution.rate.ms:60000}")
  private long rateOfJanitorExecutionMs = 1 * 60 * 1000; // 1 minute

  /******************************* Log management. *********************************************/
  @Value("${number.of.logs.for.secondary.to.sync.up:2000}")
  private int maxNumberLogsForSecondaryToSyncUp = 2000;

  // for logs secondary already have, what needs to be synced is just status,
  // so the limit could be larger
  @Value("${number.of.logs.for.secondary.to.sync.up.on.secondary.side:5000}")
  private int maxNumberLogsForSecondaryToSyncUpOnSecondarySide = 5000;

  // the volume will control the concurrent migration number in primary,
  // because the primary will read page and push
  // page to secondary, if the concurrent number is too large,
  // the pressure of primary will increase a lot.
  @Value("${number.of.copy.page.per.storage:1}")
  private int numberOfCopyPagePerStorage = 1;

  @Value("${max.number.of.miss.logs.per.sync:10000}")
  private int maxNumberOfMissLogsPerSync = 10000;

  @Value("${give.me.your.logs.count.each.time:100}")
  private int giveMeYourLogsCountEachTime = 100;

  @Value("${plal.number.page.to.apply.per.driver:100}")
  private int maxNumberOfPagesToApplyPerDrive = 100;

  @Value("${ppl.number.log.to.persist.per.driver:2048}")
  private int maxNumberOfLogsToPersistPerDrive = 2048;

  @Value("${pcl.probability.to.synclogs.when.no.enough.logs:50}")
  private int probabilityToSyncLogsWhenNoEnoughLogs = 50;

  @Value("${max.gap.of.pcl.logging.for.sync.logs:51200}")
  private int maxGapOfPclLoggingForSyncLogs = 50 * 1024;

  /************** Timeout for clean up completing log at the secondary segment unit. ***/
  @Value("${core.of.clean.up.timeout.logs.executor.threads:2}")
  private int coreOfCleanUpTimeoutLogsExecturoThreads = 2;

  @Value("${max.of.clean.up.timeout.logs.executor.threads:10}")
  private int maxOfCleanUpTimeoutLogsExecturoThreads = 10;

  @Value("${period.clean.up.timeout.logs.per.archive:10000}")
  private int periodCleanUpTimeoutLogsPerArchive = 10000;

  @Value("${timeout.of.clean.up.completing.logs.ms:600000}")
  private int timeoutOfCleanUpCompletingLogsMs = 1000 * 60 * 10;

  @Value("${timeout.of.clean.up.crash.out.uuid.ms:1800000}")
  private int timeoutOfCleanUpCrashOutUuidMs = 1000 * 60 * 30;

  /*********** Timeout for clearing up peers' pl and cl at the primary segment unit. ***/
  // how long we have to wait to recruit a new secondary
  @Value("${threshold.to.request.for.new.member.ms:900000}")
  private int thresholdToRequestForNewMemberMs = 15 * 60 * 1000; // 15 minutes
  @Value("${threshold.of.missing.pages.to.request.for.new.member.ms:50000}")
  private int thresholdOfMissingPagesToRequestForNewMember = 50000;
  // the missing logs threshold should be set to a very large value near the max number of
  // logs we will have in both memory and log storage files
  @Value("${threshold.of.missing.logs.to.request.for.new.member.ms:500000}")
  private long thresholdOfMissingLogsToRequestForNewMember = 500000;
  private int thresholdToClearSecondaryPlMs = thresholdToRequestForNewMemberMs;
  // same as secondary lease at primary
  private int thresholdToClearSecondaryClMs = secondaryLeaseInPrimaryMs;

  @Value("${threshold.to.remove.ejected.archive.ms:86400000}")
  private long thresholdToRemoveEjectedArchive = 24 * 60 * 60 * 1000L; // 1 day

  @Value("${max.async.connections.per.datanode.endpoint:10}")
  private int maxConnectionsPerAsyncDataNodeEndPoint = 10;
  @Value("${max.sync.connections.per.datanode.endpoint:10}")
  private int maxConnectionsPerSyncDataNodeEndPoint = 10;

  @Value("${min.service.workers:0}")
  private int minNumWorkerThreads = 0;
  @Value("${max.service.workers:0}")
  private int maxNumWorkerThreads = 0;

  @Value("${core.pool.size.for.catchup.log.engine.pcl:10}")
  private int corePoolSizeForCatchupLogEnginePcl = 10;

  @Value("${max.pool.size.for.catchup.log.engine.pcl:40}")
  private int maxPoolSizeForCatchupLogEnginePcl = 40;

  @Value("${core.pool.size.for.catchup.log.engine.ppl:0}")
  private int corePoolSizeForCatchupLogEnginePpl = 0;

  @Value("${max.pool.size.for.catchup.log.engine.ppl:40}")
  private int maxPoolSizeForCatchupLogEnginePpl = 40;

  @Value("${core.pool.size.for.state.processing.engine.other:10}")
  private int corePoolSizeForStateProcessingEngineOther = 10;

  @Value("${max.pool.size.for.state.processing.engine.other:50}")
  private int maxPoolSizeForStateProcessingEngineOther = 50;

  @Value("${core.pool.size.for.state.processing.engine.expiration.checker:5}")
  private int corePoolSizeForStateProcessingEngineExpirationChecker = 5;

  @Value("${max.pool.size.for.state.processing.engine.expiration.checker:5}")
  private int maxPoolSizeForStateProcessingEngineExpirationChecker = 5;

  @Value("${core.pool.size.for.state.processing.engine.heartbeat:10}")
  private int corePoolSizeForStateProcessingEngineHeartbeat = 10;

  @Value("${max.pool.size.for.state.processing.engine.heartbeat:30}")
  private int maxPoolSizeForStateProcessingEngineHeartbeat = 30;

  @Value("${thread.pool.size.of.rollback:3}")
  private int threadPoolSizeOfRollback = 3;

  @Value("${shadow.page.deleter.rate.ms:2000}")
  private int rateOfShadowPageDeleterInMs = 2000;

  @Value("${max.channel.pending.size.mb:200}")
  private int maxChannelPendingSizeMb = 200;

  /**
   * check storage whether change, such as disk plugin or plugout.
   */
  @Value("${storage.check.rate.ms:60000}")
  private int storageCheckRateMs = 60000; // 60s

  /**
   * persist segment unit meta data.
   */
  @Value("${segment.unit.metadata.persist.rate.ms:60000}")
  private int persistSegUnitMetadataRateMs = 60000; // 60s

  @Value("${write.storage.io.step:4}")
  private int writeStorageIoStep = 4;

  @Value("${read.storage.io.step:4}")
  private int readStorageIoStep = 4;

  /**
   * max size per IO request, the value should be less the 1M, because the default max frame size of
   * network is 1M.
   */
  @Value("${max.io.data.size.per.request.bytes:1048576}")
  private int maxIoDataSizePerRequestBytes = 1048576;

  @Value("${max.io.pending.requests:1000}")
  private int maxIoPendingRequests = 1000;

  @Value("${storage.io.check.rate.second:300}")
  private int storageIoCheckRateSecond = 300;

  @Value("${storage.io.count.in.sample:1000}")
  private int storageIoCountInSample = 1000;

  @Value("${storage.io.sample.count:20}")
  private int storageIoSampleCount = 20;

  /**
   * thread pool size configuration.
   */
  @Value("${max.number.of.plal.worker.pending.page:1024}")
  private int maxNumberOfPlalWorkerPendingPage = 1024;

  @Value("${concurrent.page.contexts.per.plal.worker:32}")
  private int concurrentPageContextsPerPlalWorker = 32;

  @Value("${max.threadpool.size.per.storage:4}")
  private int maxThreadpoolSizePerStorage = 4;

  @Value("${max.io.depth.per.hdd.storage:32}")
  private int maxIoDepthPerHddStorage = 32;

  @Value("${max.io.depth.per.ssd.storage:32}")
  private int maxIoDepthPerSsdStorage = 32;

  @Value("${number.of.dirty.page.per.flush.request:16}")
  private int numberOfDirtyPagePerFlushRequest = 16;

  @Value("${wait.for.memory.buffer.timeout.ms:50}")
  private int waitForMemoryBufferTimeoutMs = 50;

  @Value("${max.synchronize.time.for.create.log.ms:1000}")
  private int maxSynchronizeTimeForCreateLogMs = 1000;

  @Value("${fast.buffer.allocate.aligned:0}")
  private int fastBufferAllocateAligned = 0;

  @Value("${number.of.buffers.for.missing.log:10}")
  private int numberOfBuffersForMissingLog = 10;

  @Value("${primary.fast.buffer.percentage:30}")
  private int primaryFastBufferPercentage = 30;

  @Value("${secondary.fast.buffer.percentage:60}")
  private int secondaryFastBufferPercentage = 60;

  @Value("${sync.log.fast.buffer.percentage:10}")
  private int syncLogFastBufferPercentage = 10;

  // the smallest unit of allocation in fast buffer manager
  @Value("${fast.buffer.alignment.bytes:4096}")
  private int fastBufferAlignmentBytes = 4096;

  // TF time-first
  // PF position-first
  // MF mix-consideration
  @Value("${raw.dirty.page.selection.strategy:PF}")
  private String rawDirtyPageSelectionStrategy = "PF";

  @Value("${indexer.db.path:indexerdb}")
  private String indexerDbPath;

  /*
   * Following options is suggested by document from Rocks DB WiKi web site:
   *
   * https://github.com/facebook/rocksdb/wiki/Set-Up-Options
   */
  @Value("${indexer.max.background.compactions:4}")
  private int indexerMaxBgCompactions = 4;

  @Value("${indexer.max.background.flushes:2}")
  private int indexerMaxBgFlushes = 2;

  @Value("${indexer.bytes.per.sync:1048576}")
  private long indexerBytesPerSync = 1048576;

  @Value("${indexer.block.size:16384}")
  private long indexerBlockSize = 16384;

  @Value("${indexer.write.buffer.size.per.archive:67108864}")
  private long indexerWriteBufferSizePerArchive = 67108864L;

  /**
   * Valid values are names of instance in {@link CompressionType}.
   */
  @Value("${indexer.compression.type:no_compression}")
  private String indexerCompressionType = CompressionType.NO_COMPRESSION.name();

  @Value("${indexer.retry.max:3}")
  private int indexerRetryMax = 3;

  @Value("${indexer.retry.interval.ms:1000}")
  private long indexerRetryIntervalMs = 1000;

  @Value("${indexer.level.compaction.dynamic.level.bytes:true}")
  private boolean indexerLevelCompactionDynamicLevelBytes = true;

  /*
   * Customized
   */
  @Value("${indexer.max.total.wal.size:1073741824}")
  private long indexerMaxTotalWalSize = 10737418240L; // 10G

  @Value("${indexer.deletion.batch.limit:100000}")
  private int indexerDeletionBatchLimit;

  @Value("${indexer.deletion.delay.sec:600}")
  private int indexerDeletionDelaySec;

  @Value("${indexer.max.wait.persist.log.number.once.sync:10000}")
  private int indexerMaxPersistLogNumberWaitOnceSync;

  @Value("${indexer.persist.periodic.ms:1000}")
  private int indexerPersistLease;

  @Value("${indexer.enable.tracing:false}")
  private boolean indexerEnableTracing = false;

  @Value("${indexer.tracing.level:2}")
  private int indexerTracingLevel = 2; // 1 lower level, 2 middle, 3 higher level

  @Value("${indexer.discard.positive.history.period:0}")
  private int indexerDiscardPositiveHistoryPeriod = 0;

  @Value("${indexer.rocksdb.log.level:WARN_LEVEL}")
  private String indexerRocksdbLogLevel = InfoLogLevel.WARN_LEVEL.name();
  @Value("${indexer.rocksdb.log.max.log.file.size:1073741824}")
  private int indexerRocksdbMaxLogFileSize = 1073741824;  // 1G
  @Value("${indexer.rocksdb.log.keep.log.file.number:5}")
  private int indexerRocksdbKeepLogFileNumber = 5;

  // ========================== Configuration for indexer ===============

  @Value("${indexer.max.transactions:1000}")
  private int indexerMaxTransactions;

  @Value("${indexer.max.flushers:10}")
  private int indexerMaxFlushers;

  @Value("${indexer.flusher.bundle.size:800}")
  private int indexerFlusherBundleSize;

  @Value("${indexer.flusher.bundle.timeout.sec: 2}")
  private long indexerFlusherBundleTimeoutSec;

  @Value("${indexer.log.root:var/indexerLog}")
  private String indexerLogRoot;

  // 100MB
  @Value("${indexer.per.log.max.size.bytes:104857600}")
  private int indexerMaxSizePerLog;

  @Value("${indexer.enable.debugging.mode:false}")
  private boolean indexerDebuggingEnabled = false;

  @Value("${indexer.max.opened.log.files:10}")
  private int indexerMaxOpenedLogFiles;

  @Value("${indexer.max.lazy.finalizations:1}")
  private int indexerMaxLazyFinalizations = 1;

  @Value("${indexer.max.entries.per.deletion:100}")
  private int indexerMaxEntriesPerDeletion = 100;

  @Value("${indexer.enable.strictly.sync.log:true}")
  private boolean enableIndexerStrictlySyncLog = true;

  @Value("${indexer.retry.interval.sec.after.storage.exception:0}")
  private long indexerRetryIntervalSecAfterStorageException = 0;

  @Value("${indexer.retry.times.after.storage.exception: 3}")
  private int indexerRetryTimesAfterStorageException = 3;

  @Value("${page.count.in.raw.chunk:16}")
  private int pageCountInRawChunk = 16;

  @Value("${max.copy.page.network.iops:100000}")
  private int maxCopyPageNetworkIoPs = 100000;

  @Value("${max.copy.page.storage.iops:30}")
  private int maxCopyPageStorageIoPs = 30;

  @Value("${max.persist.data.to.disk.iops:10}")
  private int maxPersistDataToDiskIoPs = 10;

  @Value("${max.push.interval.time.ms:10000}")
  private int maxPushIntervalTimeMs = 10000;

  // it is used for secondary to tell primary if the secondary log can be caught up by primary.
  @Value("${max.log.id.window.size:5000}")
  private int maxLogIdWindowSize = 5000;

  @Value("${max.log.id.window.split.count:50}")
  private int maxLogIdWindowSplitCount = 50;

  @Value("${shutdown.save.log.dir:var/storage/savelog}")

  private String shutdownSaveLogDir = "var/storage/savelog";

  @Value("${backup.database.info.dir:var/storage/backDBInfo}")
  private String backupDbInfoDir = "var/storage/backDBInfo";

  @Value("${max.wait.for.datanode.shutdown.time.second:480}")
  private int maxWaitForDnShutdownTimeSec = 480;

  @Value("${copy.page.timeout.ms:30000}")
  private int copyPageTimeoutMs = 30000;

  // -------------------------------------- for ssd performance ----------------------------------
  @Value("${ssd.dirty.page.selection.strategy:PF}")
  private String ssdDirtyPageSelectionStrategy = "PF";

  @Value("${max.threadpool.size.per.ssd: 16}")
  private int maxThreadpoolSizePerSsd = 16;

  // how long should a dirty page stay dirty in memory cache layer in case it is requested again
  @Value("${wait.ms.to.flush.dirty.pages:0}")
  private int flushWaitMs = 0; // 0 seconds

  @Value("${max.number.of.plal.worker.pending.page.for.ssd:4096}")
  private int maxNumberOfPlalWorkerPendingPageForSsd = 4096;

  @Value("${number.of.dirty.page.per.flush.request.for.ssd:32}")
  private int numberOfDirtyPagePerFlushRequestForSsd = 32;

  @Value("${read.cache.page.ratio: 50}")
  private int readCachePageRatio = 50;

  /**
   * set the algorithm to calculate the checksum of data and metadata, you can select from DUMMY,
   * ALDER32, MD5,CRC32,CRC32C now, default is CRC32.
   */
  @Value("${page.checksum.algorithm:CRC32}")
  private String pageChecksumAlgorithm = "CRC32";

  @Value("${network.checksum.algorithm:DIGEST}")
  private String networkChecksumAlgorithm = "DIGEST";

  @Value("${page.system.memory.cache.size:0m}")
  private String pageSystemMemoryCacheSize = "0m";

  @Value("${dynamic.shadow.page.switch:false}")
  private boolean dynamicShadowPageSwitch = false;

  // ---------------------- Heap memory Size ---------------
  @Value("${memory.in.heap.size.mb:100}")
  private int memoryInHeapSizeMb = 100;

  @Value("${timeout.ms.wait.for.secondary.log.coming:1000}")
  private int timeoutWaitForSecondaryLogComing = 1000;

  @Value("${give.you.log.id.timeout:200}")
  private int giveYouLogIdTimeout = 200;

  // ---------------------- Space required for log ---------------
  @Value("${log.dirs:logs/,var/}")
  private String logDirs = "logs/,var/";

  @Value("${min.required.log.size.mb:500}")
  private int minRequiredLogSizeMb = 500;

  @Value("${min.reserved.disk.size.mb:200}")
  private int minReservedDiskSizeMb = 200;

  @Value("${network.health.checker.rate.ms:1000}")
  private int networkHealthCheckerRateMs = 1000;

  // should be larger than the lease span
  @Value("${network.revive.threshold.ms:20000}")
  private long networkReviveThresholdMs = 20000;

  @Value("${delay.record.storage.exception.ms:10000}")
  private int delayRecordStorageExceptionMs = 10000;

  @Value("${target.io.lentency.in.datanode.ms:10}")
  private int targetIoLatencyInDataNodeMs = 10;

  @Value("${ping.host.timeout.ms:50}")
  private int pingHostTimeoutMs = 50;

  @Value("${network.connection.detect.retry.maxtimes:5}")
  private int networkConnectionDetectRetryMaxtimes = 5;

  @Value("${network.connection.check.delay.ms:10000}")
  private int networkConnectionCheckDelayMs = 10000;

  @Value("${network.connection.detect.server.listening.port:54321}")
  private int networkConnectionDetectServerListeningPort = 54321;

  @Value("${network.connection.detect.server.using.native.or.not:true}")
  private boolean networkConnectionDetectServerUsingNativeOrNot = true;

  // ----------------write cache random and sequential condition
  /* if you want all io into ssd cache, set this more more..etc. 65536 */
  @Value("${cache.storage.io.sequential.condition:8}")
  private int cacheStorageIoSequentialCondition = 8;

  @Value("${need.retry.broadcast.when.become.primary:true}")
  private boolean needRetryBroadCastWhenBecomePrimary = true;
  // ------------------------io timeout ----------------------
  /* for async storage, if one IO cost ms over this digit, return StorageException */
  @Value("${storage.io.timeout:10000}")
  private int storageIoTimeout = 10000;
  // --------------- begin sync log ----------------------
  @Value("${sync.log.task.executor.threads:20}")
  private int syncLogTaskExecutorThreads = 20;
  @Value("${sync.log.package.frame.size:1024}")
  private int syncLogPackageFrameSize = 1024;
  @Value("${backward.sync.log.package.frame.size:20480}")
  private int backwardSyncLogPackageFrameSize = 20480;
  @Value("${sync.log.package.response.wait.time.ms:2000}")
  private int syncLogPackageResponseWaitTimeMs = 2000;
  @Value("${sync.log.package.wait.time.ms:10}")
  private int syncLogPackageWaitTimeMs = 10;
  @Value("${sync.log.max.wait.process.time.ms:2000}")
  private int syncLogMaxWaitProcessTimeMs = 2000;
  @Value("${secondary.wide.log.id.using.min.log.id.flag:false}")
  private boolean swlogIdUsingMinLogIdOfSencondarys = false;

  /// add by qiucy at 2018-01-11 for save the write
  // cache index (between write page and write chunk) to the rocks db
  @Value("${ssd.cache.rocksdb.path:rocks_db_ssd_cache}")
  private String ssdCacheRocksDbPath = "rocks_db_ssd_cache";

  @Value("${brick.pre.allocator.upper.threshold:20}")
  private int brickPreAllocatorUpperThreshold = 20;
  @Value("${brick.pre.allocator.lower.threshold:10}")
  private int brickPreAllocatorLowerThreshold = 10;

  @Value("${secondary.pcl.driver.timeout.ms:5000}")
  private int secondaryPclDriverTimeoutMs = 5000; // ms
  @Value("${secondary.sync.log.timeout.ms:8000}")
  private int secondarySyncLogTimeoutMs = 8000; // ms
  private RocksDbPathConfig rocksDbPathConfig = null;
  /*************Mobile slow disk checking.***************/
  @Value("${mark.slow.disk:false}")
  private boolean markSlowDisk = false;
  @Value("${start.slow.disk.await.checker:false}")
  private boolean startSlowDiskAwaitChecker = false;
  @Value("${iostat.file.name:/proc/diskstats}")
  private String iostatFileName = "/proc/diskstats";
  @Value("${slow.disk.await.threshold.ms:900}")
  private int slowDiskAwaitThreshold = 900;
  @Value("${slow.disk.await.check.interval.ms:3000}")
  private int slowDiskAwaitCheckInterval = 3000;
  /* for first strategy, the digit default: 15000000000ns (1500ms) */
  @Value("${slow.disk.latency.threshold.random.ns:1500000000}")
  private long slowDiskLatencyThresholdRandomNs = 1500000000;
  /* for first strategy, the digit default: 900000000ns (600ms) */
  @Value("${slow.disk.latency.threshold.sequential.ns:600000000}")
  private long slowDiskLatencyThresholdSequentialNs = 600000000;
  @Value("${archive.init.slow.disk.checker.enabled:false}")
  private boolean archiveInitSlowDiskCheckerEnabled = false;
  @Value("${archive.init.disk.threshold.sequential:15000}")
  private int archiveInitDiskThresholdSequential = 15000;
  @Value("${archive.init.disk.threshold.random:120}")
  private int archiveInitDiskThresholdRandom = 120;
  /* sample of slow disk if less than it, ignore */
  @Value("${slow.disk.ignore.ratio:0.60}")
  private double slowDiskIgnoreRatio = 0.60;
  /* strategy fro slow disk checking.
  0: disable checking,
  1: checking by await,
  2: checking by io cost time */
  @Value("${slow.disk.strategy:0}")
  private int slowDiskStrategy = 2;
  @Value("${enable.merge.read.on.timeout:false}")
  private boolean enableMergeReadOnTimeout = false;
  @Value("${archive.init.mode:append}")
  private String archiveInitMode = "append";
  @Value("${enable.file.buffer:false}")
  private boolean enableFileBuffer = false;
  @Value("${wait.file.buffer.timeout.ms:0}")
  private int waitFileBufferTimeOutMs = 0;
  @Value("${max.page.count.per.plal.schedule:4}")
  private int maxPageCountPerPlalSchedule = 4;
  @Value("${enable.performance.report:false}")
  private boolean enablePerformanceReport = false;
  @Value("${invalidate.page.manager.in.plal:false}")
  private boolean invalidatePageManagerInPlal;
  @Value("${write.thread.count:12}")
  private int writeThreadCount = 12;
  @Value("${raw.app.name:RAW}")
  private String rawAppName = "RAW";
  @Value("${write.token.count:116}")
  private int writeTokenCount = 116;
  @Value("${wait.for.broadcast.log.id:true}")
  private boolean waitForBroadcastLogId = true;
  @Value("${broadcast.log.id:true}")
  private boolean broadcastLogId = true;
  @Value("${primary.commit.log:true}")
  private boolean primaryCommitLog = true;
  @Value("${become.primary.thread.pool.size:1000}")
  private int becomePrimaryThreadPoolSize = 1000;
  @Value("${arbiter.count.limit.in.one.archive:500}")
  private int arbiterCountLimitInOneArchive = 500;
  @Value("${flexible.count.limit.in.one.archive:500}")
  private int flexibleCountLimitInOneArchive = 500;
  @Value("${arbiter.count.limit.in.one.datanode:2000}")
  private int arbiterCountLimitInOneDatanode = 2000;
  @Value("${concurrent.copy.page.task.count:1}")
  private int concurrentCopyPageTaskCount = 1;
  @Value("${smart.max.copy.speed.mb}")
  private int smartMaxCopySpeedMb = 100;
  @Value("${io.delay.timeunit:MILLISECONDS}")
  private String ioDelayTimeunit = "MILLISECONDS";
  @Value("${max.page.count.per.push:128}")
  private int maxPageCountPerPush = 128;
  @Value("${level.two.flush.throttle.page.count:256}")
  private int levelTwoFlushThrottlePageCount = 256;

  @Value("${write.observe.area.iops.threshold:100}")
  private int writeObserveAreaIoPsThreshold = 100;

  @Value("${read.observe.area.iops.threshold:1}")
  private int readObserveAreaIoPsThreshold = 1;
  @Value("${available.bucket.threshold.to.start.force.flush:0.1f}")
  private float availableBucketThresholdToStartForceFlush = 0.1f;
  @Value("${flush.read.mbps.threshold:200}")
  private int flushReadMbpsThreshold = 200;

  @Value("${io.task.threshold.ms:1000}")
  private long ioTaskThresholdMs = 1000L;
  @Value("${io.write.task.threshold.ms:5000}")
  private long ioWriteTaskThresholdMs = 5000L;
  @Value("${enable.io.cost.warning:false}")
  private boolean enableIoCostWarning = false;
  @Value("${enable.log.counter:false}")
  private boolean enableLogCounter = false;
  @Value("${page.metadata.size:512}")
  private int pageMetadataSize = 512;
  @Value("${page.metadata.need.flush.to.disk:true}")
  private boolean pageMetadataNeedFlushToDisk = true;
  @Value("${read.page.timeout.ms:2000}")
  private long readPageTimeoutMs = 2000;
  @Value("${disable.system.output:true}")
  private boolean disableSystemOutput = true;
  @Value("${logs.count.warning.threshold:10000}")
  private int logsCountWarningThreshold = 10000;
  // we have 3 warnings : logs after pcl; logs from plal to pcl; logs from ppl to plal;
  // and the warning level is an integer converted from a 3-bit binary code from 000 (0) to 111 (7)
  @Value("${logs.count.warning.level:0}")
  private int logsCountWarningLevel = 0;
  // 0 for self adaption
  @Value("${page.system.count:0}")
  private int pageSystemCount = 0;
  @Value("${catch.up.log.engines.count:4}")
  private int catchUpLogEnginesCount = 4;
  @Value("${state.processing.engines.count:4}")
  private int stateProcessingEnginesCount = 4;
  @Value("${min.thread.pool.size.for.segment.unit.task.executors:4}")
  private int minThreadPoolSizeForSegmentUnitTaskExecutors = 4;
  @Value("${enable.io.tracing:false}")
  private boolean enableIoTracing = false;
  @Value("${enable.front.io.cost.warning:false}")
  private boolean enableFrontIoCostWarning = false;
  @Value("${front.io.task.threshold.ms:2000}")
  private long frontIoTaskThresholdMs = 2000;
  @Value("${alarm.report.rate.ms:5000}")
  private long alarmReportRateMs = 5000;
  @Value("${secondary.first.voting.timeout.ms:30000}")
  private long secondaryFirstVotingTimeoutMs = 30000;

  @Value("${enable.clean.db.after.flushed.bucket:false}")
  private boolean enableCleanDbAfterFlushedBucket = false;

  @Value("${skipping.read.double.check:false}")
  private boolean skippingReadDoubleCheck = false;
  @Value("${system.exit.on.persist.log.error:true}")
  private boolean systemExitOnPersistLogError = true;
  private boolean forceFullCopy = false;
  @Value("${enable.logger.tracer:false}")
  private boolean enableLoggerTracer = false;
  @Value("${archive.id.record.path:var/storage/archiveIdRecord/}")
  private String archiveIdRecordPath = "var/storage/archiveIdRecord";
  @Value("${block.voting.when.migrating.secondary.has.max.pcl:true}")
  private boolean blockVotingWhenMigratingSecondaryHasMaxPcl = true;
  @Value("${sync.persist.membership.on.io.path:true}")
  private boolean arbiterSyncPersistMembershipOnIoPath = true;
  @Value("${allocate.fast.buffer.debug.latency.threshold:500}")
  private int allocateFastBufferDebugLatencyThreshold = 500;

  public DataNodeConfiguration() {
  }

  @PostConstruct
  public void postInit() {
    ArchiveIdFileRecorder.IMPROPERLY_EJECTED_RAW.init(archiveIdRecordPath);
  }

  public String getArbiterStoreFile() {
    return arbiterStoreFile;
  }

  public void setArbiterStoreFile(String arbiterStoreFile) {
    this.arbiterStoreFile = arbiterStoreFile;
  }

  public int getCacheStorageIoSequentialCondition() {
    return cacheStorageIoSequentialCondition;
  }

  public void setCacheStorageIoSequentialCondition(int cacheStorageIoSequentialCondition) {
    this.cacheStorageIoSequentialCondition = cacheStorageIoSequentialCondition;
  }

  public int getStorageIoTimeout() {
    return storageIoTimeout;
  }

  public void setStorageIoTimeout(int storageIoTimeout) {
    this.storageIoTimeout = storageIoTimeout;
  }

  public int getSyncLogTaskExecutorThreads() {
    return syncLogTaskExecutorThreads;
  }

  public void setSyncLogTaskExecutorThreads(int syncLogTaskExecutorThreads) {
    this.syncLogTaskExecutorThreads = syncLogTaskExecutorThreads;
  }

  public int getSyncLogPackageFrameSize() {
    return syncLogPackageFrameSize;
  }

  public void setSyncLogPackageFrameSize(int syncLogPackageFrameSize) {
    this.syncLogPackageFrameSize = syncLogPackageFrameSize;
  }

  public int getBackwardSyncLogPackageFrameSize() {
    return backwardSyncLogPackageFrameSize;
  }

  public void setBackwardSyncLogPackageFrameSize(int backwardSyncLogPackageFrameSize) {
    this.backwardSyncLogPackageFrameSize = backwardSyncLogPackageFrameSize;
  }

  public int getSyncLogPackageWaitTimeMs() {
    return syncLogPackageWaitTimeMs;
  }

  public void setSyncLogPackageWaitTimeMs(int syncLogPackageWaitTimeMs) {
    this.syncLogPackageWaitTimeMs = syncLogPackageWaitTimeMs;
  }

  public int getSyncLogPackageResponseWaitTimeMs() {
    return syncLogPackageResponseWaitTimeMs;
  }

  public void setSyncLogPackageResponseWaitTimeMs(int syncLogPackageResponseWaitTimeMs) {
    this.syncLogPackageResponseWaitTimeMs = syncLogPackageResponseWaitTimeMs;
  }

  public int getSyncLogMaxWaitProcessTimeMs() {
    return syncLogMaxWaitProcessTimeMs;
  }

  public void setSyncLogMaxWaitProcessTimeMs(int syncLogMaxWaitProcessTimeMs) {
    this.syncLogMaxWaitProcessTimeMs = syncLogMaxWaitProcessTimeMs;
  }

  public boolean isSwlogIdUsingMinLogIdOfSencondarys() {
    return swlogIdUsingMinLogIdOfSencondarys;
  }

  public void setSwlogIdUsingMinLogIdOfSencondarys(boolean swlogIdUsingMinLogIdOfSencondarys) {
    this.swlogIdUsingMinLogIdOfSencondarys = swlogIdUsingMinLogIdOfSencondarys;
  }

  public int getSecondarySyncLogTimeoutMs() {
    return secondarySyncLogTimeoutMs;
  }

  public void setSecondarySyncLogTimeoutMs(int secondarySyncLogTimeoutMs) {
    this.secondarySyncLogTimeoutMs = secondarySyncLogTimeoutMs;
  }

  public int getSecondaryPclDriverTimeoutMs() {
    return secondaryPclDriverTimeoutMs;
  }

  public void setSecondaryPclDriverTimeoutMs(int secondaryPclDriverTimeoutMs) {
    this.secondaryPclDriverTimeoutMs = secondaryPclDriverTimeoutMs;
  }

  public boolean isNeedRetryBroadCastWhenBecomePrimary() {
    return needRetryBroadCastWhenBecomePrimary;
  }

  public void setNeedRetryBroadCastWhenBecomePrimary(
      boolean needRetryBroadCastWhenBecomePrimary) {
    this.needRetryBroadCastWhenBecomePrimary = needRetryBroadCastWhenBecomePrimary;
  }

  public RocksDbPathConfig getRocksDbPathConfig() {
    return rocksDbPathConfig;
  }

  public void setRocksDbPathConfig(RocksDbPathConfig rocksDbPathConfig) {
    this.rocksDbPathConfig = rocksDbPathConfig;
  }

  public boolean isMarkSlowDisk() {
    return markSlowDisk;
  }

  public boolean isStartSlowDiskAwaitChecker() {
    return startSlowDiskAwaitChecker;
  }

  public String getIostatFileName() {
    return iostatFileName;
  }

  public int getSlowDiskAwaitThreshold() {
    return slowDiskAwaitThreshold;
  }

  public int getSlowDiskAwaitCheckInterval() {
    return slowDiskAwaitCheckInterval;
  }

  public int getSlowDiskStrategy() {
    return slowDiskStrategy;
  }

  public String getRawAppName() {
    return rawAppName;
  }

  public int getWriteTokenCount() {
    return writeTokenCount;
  }

  public void setWriteTokenCount(int writeTokenCount) {
    this.writeTokenCount = writeTokenCount;
  }

  public int getNetworkConnectionDetectServerListeningPort() {
    return networkConnectionDetectServerListeningPort;
  }

  public void setNetworkConnectionDetectServerListeningPort(
      int networkConnectionDetectServerListeningPort) {
    this.networkConnectionDetectServerListeningPort = networkConnectionDetectServerListeningPort;
  }

  public boolean isNetworkConnectionDetectServerUsingNativeOrNot() {
    return networkConnectionDetectServerUsingNativeOrNot;
  }

  public void setNetworkConnectionDetectServerUsingNativeOrNot(
      boolean networkConnectionDetectServerUsingNativeOrNot) {
    this.networkConnectionDetectServerUsingNativeOrNot =
        networkConnectionDetectServerUsingNativeOrNot;
  }

  public int getConcurrentCopyPageTaskCount() {
    return concurrentCopyPageTaskCount;
  }

  public void setConcurrentCopyPageTaskCount(int concurrentCopyPageTaskCount) {
    this.concurrentCopyPageTaskCount = concurrentCopyPageTaskCount;
  }

  public int getSmartMaxCopySpeedMb() {
    return smartMaxCopySpeedMb;
  }

  public void setSmartMaxCopySpeedMb(int smartMaxCopySpeedMb) {
    this.smartMaxCopySpeedMb = smartMaxCopySpeedMb;
  }

  public boolean isPageMetadataNeedFlushToDisk() {
    return pageMetadataNeedFlushToDisk;
  }

  public int getMaxPageCountPerPush() {
    return maxPageCountPerPush;
  }

  public void setMaxPageCountPerPush(int maxPageCountPerPush) {
    this.maxPageCountPerPush = maxPageCountPerPush;
  }

  public boolean isForceFullCopy() {
    return forceFullCopy;
  }

  public void setForceFullCopy(boolean forceFullCopy) {
    this.forceFullCopy = forceFullCopy;
  }

  public int getBecomePrimaryThreadPoolSize() {
    return becomePrimaryThreadPoolSize;
  }

  public void setBecomePrimaryThreadPoolSize(int becomePrimaryThreadPoolSize) {
    this.becomePrimaryThreadPoolSize = becomePrimaryThreadPoolSize;
  }

  public int getTargetIoLatencyInDataNodeMs() {
    return targetIoLatencyInDataNodeMs;
  }

  public void setTargetIoLatencyInDataNodeMs(int targetIoLatencyInDataNodeMs) {
    this.targetIoLatencyInDataNodeMs = targetIoLatencyInDataNodeMs;
  }

  public int getDelayRecordStorageExceptionMs() {
    return delayRecordStorageExceptionMs;
  }

  public void setDelayRecordStorageExceptionMs(int delayRecordStorageExceptionMs) {
    this.delayRecordStorageExceptionMs = delayRecordStorageExceptionMs;
  }

  public int getAllocateFastBufferDebugLatencyThreshold() {
    return allocateFastBufferDebugLatencyThreshold;
  }

  public void setAllocateFastBufferDebugLatencyThreshold(
      int allocateFastBufferDebugLatencyThreshold) {
    this.allocateFastBufferDebugLatencyThreshold = allocateFastBufferDebugLatencyThreshold;
  }

  public int getMaxCopyPageStorageIoPs() {
    return maxCopyPageStorageIoPs;
  }

  public void setMaxCopyPageStorageIoPs(int maxCopyPageStorageIoPs) {
    this.maxCopyPageStorageIoPs = maxCopyPageStorageIoPs;
  }

  public int getNumberOfCopyPagePerStorage() {
    return numberOfCopyPagePerStorage;
  }

  public void setNumberOfCopyPagePerStorage(int numberOfCopyPagePerStorage) {
    this.numberOfCopyPagePerStorage = numberOfCopyPagePerStorage;
  }

  public String getLogDirs() {
    return logDirs;
  }

  public void setLogDirs(String logDirs) {
    this.logDirs = logDirs;
  }

  public int getMaxNumberOfMissLogsPerSync() {
    return maxNumberOfMissLogsPerSync;
  }

  public void setMaxNumberOfMissLogsPerSync(int maxNumberOfMissLogsPerSync) {
    this.maxNumberOfMissLogsPerSync = maxNumberOfMissLogsPerSync;
  }

  public int getMinRequiredLogSizeMb() {
    return minRequiredLogSizeMb;
  }

  public void setMinRequiredLogSizeMb(int minRequiredLogSizeMb) {
    this.minRequiredLogSizeMb = minRequiredLogSizeMb;
  }

  public int getMinReservedDiskSizeMb() {
    return minReservedDiskSizeMb;
  }

  public void setMinReservedDiskSizeMb(int minReservedDiskSizeMb) {
    this.minReservedDiskSizeMb = minReservedDiskSizeMb;
  }

  public int getTimeoutWaitForSecondaryLogComing() {
    return timeoutWaitForSecondaryLogComing;
  }

  public void setTimeoutWaitForSecondaryLogComing(int timeoutWaitForSecondaryLogComing) {
    this.timeoutWaitForSecondaryLogComing = timeoutWaitForSecondaryLogComing;
  }

  public boolean getDynamicShadowPageSwitch() {
    return dynamicShadowPageSwitch;
  }

  public String getPageChecksumAlgorithm() {
    return pageChecksumAlgorithm;
  }

  public void setPageChecksumAlgorithm(String pageChecksumAlgorithm) {
    this.pageChecksumAlgorithm = pageChecksumAlgorithm;
  }

  public String getNetworkChecksumAlgorithm() {
    return networkChecksumAlgorithm;
  }

  public void setNetworkChecksumAlgorithm(String networkChecksumAlgorithm) {
    this.networkChecksumAlgorithm = networkChecksumAlgorithm;
  }

  public String getShutdownSaveLogDir() {
    return shutdownSaveLogDir;
  }

  public void setShutdownSaveLogDir(String shutdownSaveLogDir) {
    this.shutdownSaveLogDir = shutdownSaveLogDir;
  }

  public int getPrimaryFastBufferPercentage() {
    return primaryFastBufferPercentage;
  }

  public void setPrimaryFastBufferPercentage(int primaryFastBufferPercentage) {
    this.primaryFastBufferPercentage = primaryFastBufferPercentage;
  }

  public int getWriteStorageIoStep() {
    return writeStorageIoStep;
  }

  public void setWriteStorageIoStep(int writeStorageIoStep) {
    this.writeStorageIoStep = writeStorageIoStep;
  }

  public int getReadStorageIoStep() {
    return readStorageIoStep;
  }

  public void setReadStorageIoStep(int readStorageIoStep) {
    this.readStorageIoStep = readStorageIoStep;
  }

  public long getSegmentUnitSize() {
    return storageConfiguration.getSegmentSizeByte();
  }

  public void setSegmentUnitSize(long segmentUnitSize) {
    storageConfiguration.setSegmentSizeByte(segmentUnitSize);
  }

  public void setDegradeDiskStorageExceptionThreshold(int degradeDiskStorageExceptionThreshold) {
    this.degradeDiskStorageExceptionThreshold = degradeDiskStorageExceptionThreshold;
  }

  public int getDegradeDiskStorageExceptionThreshold() {
    return degradeDiskStorageExceptionThreshold;
  }

  public int getBrokenDiskStorageExceptionThreshold() {
    return brokenDiskStorageExceptionThreshold;
  }

  public void setBrokenDiskStorageExceptionThreshold(int brokenDiskStorageExceptionThreshold) {
    this.brokenDiskStorageExceptionThreshold = brokenDiskStorageExceptionThreshold;
  }

  public ArchiveConfiguration getArchiveConfiguration() {
    return archiveConfiguration;
  }

  public void setArchiveConfiguration(ArchiveConfiguration archiveConfiguration) {
    this.archiveConfiguration = archiveConfiguration;
  }

  // no matter in memory, l2cache or raw disk,
  // the logical page size should be page data size + page metadata size
  public long getPhysicalPageSize() {
    return ArchiveOptions.PAGE_PHYSICAL_SIZE;
  }

  public int getPageSize() {
    return (int) storageConfiguration.getPageSizeByte();
  }

  public void setPageSize(int pageSize) {
    storageConfiguration.setPageSizeByte((int) pageSize);
  }

  public int getPageMetadataSize() {
    return pageMetadataSize;
  }

  public void setPageMetadataSize(int pageMetadataSize) {
    this.pageMetadataSize = pageMetadataSize;
  }

  public int getIoTimeoutMs() {
    return (int) storageConfiguration.getIoTimeoutMs();
  }

  public int getNumberOfPages() {
    return numberOfPages;
  }

  public void setNumberOfPages(int numberOfPages) {
    this.numberOfPages = numberOfPages;
  }

  public boolean isStartFlusher() {
    return startFlusher;
  }

  public void setStartFlusher(boolean startFlusher) {
    this.startFlusher = startFlusher;
  }

  public int getFlushWaitMs() {
    return flushWaitMs;
  }

  public void setFlushWaitMs(int flushWaitMs) {
    this.flushWaitMs = flushWaitMs;
  }

  public int getDefaultPageRequestTimeoutMs() {
    return defaultPageRequestTimeoutMs;
  }

  public void setDefaultPageRequestTimeoutMs(int defaultPageRequestTimeoutMs) {
    this.defaultPageRequestTimeoutMs = defaultPageRequestTimeoutMs;
  }

  public int getRateOfExtendingLeaseMs() {
    return rateOfExtendingLeaseMs;
  }

  public void setRateOfExtendingLeaseMs(int rateOfExtendingLeaseMs) {
    this.rateOfExtendingLeaseMs = rateOfExtendingLeaseMs;
  }

  public int getSecondaryLeaseMs() {
    return secondaryLeaseMs;
  }

  public void setSecondaryLeaseMs(int secondaryLeaseMs) {
    this.secondaryLeaseMs = secondaryLeaseMs;
  }

  public int getNewBornSecondaryLeaseMs() {
    return newBornSecondaryLeaseMs;
  }

  public void setNewBornSecondaryLeaseMs(int newBornSecondaryLeaseMs) {
    this.newBornSecondaryLeaseMs = newBornSecondaryLeaseMs;
  }

  public int getSecondaryLeaseInPrimaryMs() {
    return secondaryLeaseInPrimaryMs;
  }

  public void setSecondaryLeaseInPrimaryMs(int secondaryLeaseInPrimaryMs) {
    this.secondaryLeaseInPrimaryMs = secondaryLeaseInPrimaryMs;
  }

  public int getExtensionRequestTimeOutMs() {
    return extensionRequestTimeOutMs;
  }

  public void setExtensionRequestTimeOutMs(int extensionRequestTimeOutMs) {
    this.extensionRequestTimeOutMs = extensionRequestTimeOutMs;
  }

  public int getRateOfReportingArchivesMs() {
    return rateOfReportingArchivesMs;
  }

  public void setRateOfReportingArchivesMs(int rateOfReportingArchivesMs) {
    this.rateOfReportingArchivesMs = rateOfReportingArchivesMs;
  }

  public double getArchiveReservedRatio() {
    return archiveReservedRatio;
  }

  public void setArchiveReservedRatio(double archiveReservedRatio) {
    this.archiveReservedRatio = archiveReservedRatio;
  }

  public int getRateOfReportingSegmentsMs() {
    return rateOfReportingSegmentsMs;
  }

  public void setRateOfReportingSegmentsMs(int rateOfReportingSegmentsMs) {
    this.rateOfReportingSegmentsMs = rateOfReportingSegmentsMs;
  }

  public int getDataNodeRequestTimeoutMs() {
    return dataNodeRequestTimeoutMs;
  }

  public void setDataNodeRequestTimeoutMs(int dataNodeRequestTimeoutMs) {
    this.dataNodeRequestTimeoutMs = dataNodeRequestTimeoutMs;
  }

  public int getMaxNumberLogsForSecondaryToSyncUp() {
    return this.maxNumberLogsForSecondaryToSyncUp;
  }

  public void setMaxNumberLogsForSecondaryToSyncUp(int maxNumberLogsForSecondaryToSyncUp) {
    this.maxNumberLogsForSecondaryToSyncUp = maxNumberLogsForSecondaryToSyncUp;
  }

  public int getMaxConnectionsPerAsyncDataNodeEndPoint() {
    return this.maxConnectionsPerAsyncDataNodeEndPoint;
  }

  public void setMaxConnectionsPerAsyncDataNodeEndPoint(
      int maxConnectionsPerAsyncDataNodeEndPoint) {
    this.maxConnectionsPerAsyncDataNodeEndPoint = maxConnectionsPerAsyncDataNodeEndPoint;
  }

  public int getMaxConnectionsPerSyncDataNodeEndPoint() {
    return maxConnectionsPerSyncDataNodeEndPoint;
  }

  public void setMaxConnectionsPerSyncDataNodeEndPoint(int maxConnectionsPerSyncDataNodeEndPoint) {
    this.maxConnectionsPerSyncDataNodeEndPoint = maxConnectionsPerSyncDataNodeEndPoint;
  }

  public long getRequestNewSegmentUnitExpirationThresholdMs() {
    return this.requestNewSegmentUnitExpirationThresholdMs;
  }

  public void setRequestNewSegmentUnitExpirationThresholdMs(
      long requestNewSegmentUnitExpirationThresholdMs) {
    this.requestNewSegmentUnitExpirationThresholdMs = requestNewSegmentUnitExpirationThresholdMs;
  }

  public long getInteverlForCheckPageReleasedMs() {
    return this.inteverlForCheckPageReleasedMs;
  }

  public void setInteverlForCheckPageReleasedMs(long inteverlForCheckPageReleasedMs) {
    this.inteverlForCheckPageReleasedMs = inteverlForCheckPageReleasedMs;
  }

  public int getFlushPagesIntervalMs() {
    return flushPagesIntervalMs;
  }

  public void setFlushPagesIntervalMs(int flushPagesIntervalMs) {
    this.flushPagesIntervalMs = flushPagesIntervalMs;
  }

  public long getWaitTimeMsToMoveToDeleted() {
    return this.waitTimeMsToMoveToDeleted;
  }

  public void setWaitTimeMsToMoveToDeleted(long waitTimeMsToMoveToDeleted) {
    this.waitTimeMsToMoveToDeleted = waitTimeMsToMoveToDeleted;
  }

  public int getRateOfSegmentUnitDeleterDalayMs() {
    return rateOfSegmentUnitDeleterDalayMs;
  }

  public void setRateOfSegmentUnitDeleterDalayMs(int rateOfSegmentUnitDeleterDalayMs) {
    this.rateOfSegmentUnitDeleterDalayMs = rateOfSegmentUnitDeleterDalayMs;
  }

  public long getQuarantineZoneMsToCreateDeletedSegment() {
    return quarantineZoneMsToRecoverDeletingSegment;
  }

  public void setQuarantineZoneMsToCreateDeletedSegment(
      long quarantineZoneMsToCreateDeletedSegment) {
    this.quarantineZoneMsToRecoverDeletingSegment = quarantineZoneMsToCreateDeletedSegment;
  }

  public int getMinNumWorkerThreads() {
    return minNumWorkerThreads;
  }

  public void setMinNumWorkerThreads(int minNumWorkerThreads) {
    this.minNumWorkerThreads = minNumWorkerThreads;
  }

  public int getMaxNumWorkerThreads() {
    return maxNumWorkerThreads;
  }

  public void setMaxNumWorkerThreads(int maxNumWorkerThreads) {
    this.maxNumWorkerThreads = maxNumWorkerThreads;
  }

  public long getRateOfDuplicatingVolumeMetadatdaJsonMs() {
    return this.rateOfDuplicatingVolumeMetadataJsonMs;
  }

  public int getMaxNumberOfLogsToPersistPerDrive() {
    return this.maxNumberOfLogsToPersistPerDrive;
  }

  public void setMaxNumberOfLogsToPersistPerDrive(int maxNumberOfLogsToPersistPerDrive) {
    this.maxNumberOfLogsToPersistPerDrive = maxNumberOfLogsToPersistPerDrive;
  }

  public int getMaxNumberOfPagesToApplyPerDrive() {
    return maxNumberOfPagesToApplyPerDrive;
  }

  public void setMaxNumberOfPagesToApplyPerDrive(int numberOfLogsToApplyPerDrive) {
    this.maxNumberOfPagesToApplyPerDrive = numberOfLogsToApplyPerDrive;
  }

  public int getNumberOfPageCorrectors() {
    return this.numberOfPageCorrectors;
  }

  public void setNumberOfPageCorrectors(int numCorrectors) {
    this.numberOfPageCorrectors = numCorrectors;
  }

  public int getThresholdToRequestForNewMemberMs() {
    return thresholdToRequestForNewMemberMs;
  }

  public void setThresholdToRequestForNewMemberMs(int thresholdToRequestForNewMemberMs) {
    this.thresholdToRequestForNewMemberMs = thresholdToRequestForNewMemberMs;
  }

  public int getThresholdOfMissingPagesToRequestForNewMember() {
    return thresholdOfMissingPagesToRequestForNewMember;
  }

  public void setThresholdOfMissingPagesToRequestForNewMember(
      int thresholdOfMissingPagesToRequestForNewMember) {
    this.thresholdOfMissingPagesToRequestForNewMember =
        thresholdOfMissingPagesToRequestForNewMember;
  }

  public long getThresholdOfMissingLogsToRequestForNewMember() {
    return thresholdOfMissingLogsToRequestForNewMember;
  }

  public void setThresholdOfMissingLogsToRequestForNewMember(
      long thresholdOfMissingLogsToRequestForNewMember) {
    this.thresholdOfMissingLogsToRequestForNewMember = thresholdOfMissingLogsToRequestForNewMember;
  }

  public int getThresholdToClearSecondaryClMs() {
    return thresholdToClearSecondaryClMs;
  }

  public void setThresholdToClearSecondaryClMs(int thresholdToClearSecondaryClMs) {
    this.thresholdToClearSecondaryClMs = thresholdToClearSecondaryClMs;
  }

  public int getThresholdToClearSecondaryPlMs() {
    return thresholdToClearSecondaryPlMs;
  }

  public void setThresholdToClearSecondaryPlMs(int thresholdToClearSecondaryPlMs) {
    this.thresholdToClearSecondaryPlMs = thresholdToClearSecondaryPlMs;
  }

  public int getOtherStatusesLeaseMs() {
    return otherStatusesLeaseMs;
  }

  public void setOtherStatusesLeaseMs(int otherStatusesLeaseMs) {
    this.otherStatusesLeaseMs = otherStatusesLeaseMs;
  }

  public int getCorePoolSizeForCatchupLogEnginePcl() {
    return this.corePoolSizeForCatchupLogEnginePcl;
  }

  public void setCorePoolSizeForCatchupLogEnginePcl(int corePoolSizeForCatchupLogEngine) {
    this.corePoolSizeForCatchupLogEnginePcl = corePoolSizeForCatchupLogEngine;
  }

  public int getMaxPoolSizeForCatchupLogEnginePcl() {
    return this.maxPoolSizeForCatchupLogEnginePcl;
  }

  public void setMaxPoolSizeForCatchupLogEnginePcl(int maxPoolSizeForCatchupLogEngine) {
    this.maxPoolSizeForCatchupLogEnginePcl = maxPoolSizeForCatchupLogEngine;
  }

  public long getRateOfJanitorExecutionMs() {
    return rateOfJanitorExecutionMs;
  }

  public void setRateOfJanitorExecutionMs(long rateOfJanitorExecutionMs) {
    this.rateOfJanitorExecutionMs = rateOfJanitorExecutionMs;
  }

  public long getRateOfDuplicatingVolumeMetadataJsonMs() {
    return rateOfDuplicatingVolumeMetadataJsonMs;
  }

  public void setRateOfDuplicatingVolumeMetadataJsonMs(long rateOfDuplicatingVolumeMetadataJsonMs) {
    this.rateOfDuplicatingVolumeMetadataJsonMs = rateOfDuplicatingVolumeMetadataJsonMs;
  }

  public int getRateOfSegmentUnitDeleterExecutionMs() {
    return rateOfSegmentUnitDeleterExecutionMs;
  }

  public void setRateOfSegmentUnitDeleterExecutionMs(int rateOfSegmentUnitDeleterExecutionMs) {
    this.rateOfSegmentUnitDeleterExecutionMs = rateOfSegmentUnitDeleterExecutionMs;
  }

  public int getStorageCheckRateMs() {
    return storageCheckRateMs;
  }

  public void setStorageCheckRateMs(int storageCheckRateMs) {
    this.storageCheckRateMs = storageCheckRateMs;
  }

  public int getPersistSegUnitMetadataRateMs() {
    return persistSegUnitMetadataRateMs;
  }

  public void setPersistSegUnitMetadataRateMs(int persistSegUnitMetadataRateMs) {
    this.persistSegUnitMetadataRateMs = persistSegUnitMetadataRateMs;
  }

  public int getCorePoolSizeForStateProcessingEngineOther() {
    return this.corePoolSizeForStateProcessingEngineOther;
  }

  public void setCorePoolSizeForStateProcessingEngineOther(
      int corePoolSizeForStateProcessingEngine) {
    this.corePoolSizeForStateProcessingEngineOther = corePoolSizeForStateProcessingEngine;
  }

  public int getMaxPoolSizeForStateProcessingEngineOther() {
    return maxPoolSizeForStateProcessingEngineOther;
  }

  public void setMaxPoolSizeForStateProcessingEngineOther(int maxPoolSizeForStateProcessingEngine) {
    this.maxPoolSizeForStateProcessingEngineOther = maxPoolSizeForStateProcessingEngine;
  }

  public int getNumberOfDirtyPagePerFlushRequest() {
    return numberOfDirtyPagePerFlushRequest;
  }

  public void setNumberOfDirtyPagePerFlushRequest(int numberOfDirtyPagePerFlushRequest) {
    this.numberOfDirtyPagePerFlushRequest = numberOfDirtyPagePerFlushRequest;
  }

  public int getMaxIoDepthPerHddStorage() {
    return maxIoDepthPerHddStorage;
  }

  public void setMaxIoDepthPerHddStorage(int maxIoDepthPerHddStorage) {
    this.maxIoDepthPerHddStorage = maxIoDepthPerHddStorage;
  }

  public int getMaxIoDepthPerSsdStorage() {
    return maxIoDepthPerSsdStorage;
  }

  public int getMaxThreadpoolSizePerStorage() {
    return maxThreadpoolSizePerStorage;
  }

  public void setMaxThreadpoolSizePerStorage(int maxThreadpoolSizePerStorage) {
    this.maxThreadpoolSizePerStorage = maxThreadpoolSizePerStorage;
  }

  public int getMaxThreadpoolSizePerSsd() {
    return maxThreadpoolSizePerSsd;
  }

  public void setMaxThreadpoolSizePerSsd(int maxThreadpoolSizePerSsd) {
    this.maxThreadpoolSizePerSsd = maxThreadpoolSizePerSsd;
  }

  public int getMemorySizeForDataLogsMb() {
    return memorySizeForDataLogsMb;
  }

  public void setMemorySizeForDataLogsMb(int memorySizeForDataLogsMb) {
    this.memorySizeForDataLogsMb = memorySizeForDataLogsMb;
  }

  public long getWaitTimeMsFromBrokenToDeleted() {
    return waitTimeMsFromBrokenToDeleted;
  }

  public void setWaitTimeMsFromBrokenToDeleted(long waitTimeMsFromBrokenToDeleted) {
    this.waitTimeMsFromBrokenToDeleted = waitTimeMsFromBrokenToDeleted;
  }

  public boolean isNeedCheckRollbackInOffling() {
    return needCheckRollbackInOffling;
  }

  public void setNeedCheckRollbackInOffling(boolean needCheckRollbackInOffling) {
    this.needCheckRollbackInOffling = needCheckRollbackInOffling;
  }

  public int getGiveMeYourLogsCountEachTime() {
    return giveMeYourLogsCountEachTime;
  }

  public void setGiveMeYourLogsCountEachTime(int giveMeYourLogsCountEachTime) {
    this.giveMeYourLogsCountEachTime = giveMeYourLogsCountEachTime;
  }

  public int getCorePoolSizeForCatchupLogEnginePpl() {
    return corePoolSizeForCatchupLogEnginePpl;
  }

  public void setCorePoolSizeForCatchupLogEnginePpl(int corePoolSizeForCatchupLogEnginePpl) {
    this.corePoolSizeForCatchupLogEnginePpl = corePoolSizeForCatchupLogEnginePpl;
  }

  public int getMaxPoolSizeForCatchupLogEnginePpl() {
    return maxPoolSizeForCatchupLogEnginePpl;
  }

  public void setMaxPoolSizeForCatchupLogEnginePpl(int maxPoolSizeForCatchupLogEnginePpl) {
    this.maxPoolSizeForCatchupLogEnginePpl = maxPoolSizeForCatchupLogEnginePpl;
  }

  public int getCorePoolSizeForStateProcessingEngineHeartbeat() {
    return corePoolSizeForStateProcessingEngineHeartbeat;
  }

  public void setCorePoolSizeForStateProcessingEngineHeartbeat(
      int corePoolSizeForStateProcessingEngineHeartbeat) {
    this.corePoolSizeForStateProcessingEngineHeartbeat =
        corePoolSizeForStateProcessingEngineHeartbeat;
  }

  public int getMaxPoolSizeForStateProcessingEngineHeartbeat() {
    return maxPoolSizeForStateProcessingEngineHeartbeat;
  }

  public void setMaxPoolSizeForStateProcessingEngineHeartbeat(
      int maxPoolSizeForStateProcessingEngineHeartbeat) {
    this.maxPoolSizeForStateProcessingEngineHeartbeat =
        maxPoolSizeForStateProcessingEngineHeartbeat;
  }

  public int getMaxNumberOfPlalWorkerPendingPage() {
    return this.maxNumberOfPlalWorkerPendingPage;
  }

  public void setMaxNumberOfPlalWorkerPendingPage(int maxNumberOfPlalWorkerPendingPage) {
    this.maxNumberOfPlalWorkerPendingPage = maxNumberOfPlalWorkerPendingPage;
  }

  public int getMaxNumberOfPlalWorkerPendingPageForSsd() {
    return this.maxNumberOfPlalWorkerPendingPageForSsd;
  }

  public void setMaxNumberOfPlalWorkerPendingPageForSsd(
      int maxNumberOfPlalWorkerPendingPageForSsd) {
    this.maxNumberOfPlalWorkerPendingPageForSsd = maxNumberOfPlalWorkerPendingPageForSsd;
  }

  public int getMaxSynchronizeTimeForCreateLogMs() {
    return this.maxSynchronizeTimeForCreateLogMs;
  }

  public void setMaxSynchronizeTimeForCreateLogMs(int maxSynchronizeTimeForCreateLogMs) {
    this.maxSynchronizeTimeForCreateLogMs = maxSynchronizeTimeForCreateLogMs;
  }

  public int getWaitForMemoryBufferTimeoutMs() {
    return this.waitForMemoryBufferTimeoutMs;
  }

  public void setWaitForMemoryBufferTimeoutMs(int waitForMemoryBufferTimeoutMs) {
    this.waitForMemoryBufferTimeoutMs = waitForMemoryBufferTimeoutMs;
  }

  public int getFastBufferAllocateAligned() {
    return this.fastBufferAllocateAligned;
  }

  public void setFastBufferAllocateAligned(int fastBufferAllocateAligned) {
    this.fastBufferAllocateAligned = fastBufferAllocateAligned;
  }

  public int getProbabilityToSyncLogsWhenNoEnoughLogs() {
    return this.probabilityToSyncLogsWhenNoEnoughLogs;
  }

  public void setProbabilityToSyncLogsWhenNoEnoughLogs(int probabilityToSyncLogsWhenNoEnoughLogs) {
    this.probabilityToSyncLogsWhenNoEnoughLogs = probabilityToSyncLogsWhenNoEnoughLogs;
  }

  public int getExtensionConnectionTimeoutMs() {
    return extensionConnectionTimeoutMs;
  }

  public void setExtensionConnectionTimeoutMs(int extensionConnectionTimeoutMs) {
    this.extensionConnectionTimeoutMs = extensionConnectionTimeoutMs;
  }

  public int getNumberOfBuffersForMissingLog() {
    return numberOfBuffersForMissingLog;
  }

  public void setNumberOfBuffersForMissingLog(int numberOfBuffersForMissingLog) {
    this.numberOfBuffersForMissingLog = numberOfBuffersForMissingLog;
  }

  public int getMaxIoDataSizePerRequestBytes() {
    return maxIoDataSizePerRequestBytes;
  }

  public void setMaxIoDataSizePerRequestBytes(int maxIoDataSizePerRequestBytes) {
    this.maxIoDataSizePerRequestBytes = maxIoDataSizePerRequestBytes;
  }

  public int getMaxChannelPendingSizeMb() {
    return maxChannelPendingSizeMb;
  }

  public void setMaxChannelPendingSizeMb(int maxChannelPendingSizeMb) {
    this.maxChannelPendingSizeMb = maxChannelPendingSizeMb;
  }

  public int getSecondaryFastBufferPercentage() {
    return secondaryFastBufferPercentage;
  }

  public void setSecondaryFastBufferPercentage(int secondaryFastBufferPercentage) {
    this.secondaryFastBufferPercentage = secondaryFastBufferPercentage;
  }

  public int getSyncLogFastBufferPercentage() {
    return syncLogFastBufferPercentage;
  }

  public void setSyncLogFastBufferPercentage(int syncLogFastBufferPercentage) {
    this.syncLogFastBufferPercentage = syncLogFastBufferPercentage;
  }

  public int getRateOfShadowPageDeleterInMs() {
    return rateOfShadowPageDeleterInMs;
  }

  public void setRateOfShadowPageDeleterInMs(int rateOfShadowPageDeleterInMs) {
    this.rateOfShadowPageDeleterInMs = rateOfShadowPageDeleterInMs;
  }

  public int getFastBufferAlignmentBytes() {
    return fastBufferAlignmentBytes;
  }

  public void setFastBufferAlignmentBytes(int fastBufferAlignmentBytes) {
    this.fastBufferAlignmentBytes = fastBufferAlignmentBytes;
  }

  public int getIndexerFlusherBundleSize() {
    return indexerFlusherBundleSize;
  }

  public void setIndexerFlusherBundleSize(int indexerFlusherBundleSize) {
    this.indexerFlusherBundleSize = indexerFlusherBundleSize;
  }

  public long getIndexerFlusherBundleTimeoutSec() {
    return indexerFlusherBundleTimeoutSec;
  }

  public void setIndexerFlusherBundleTimeoutSec(long indexerFlusherBundleTimeoutSec) {
    this.indexerFlusherBundleTimeoutSec = indexerFlusherBundleTimeoutSec;
  }

  public int getIndexerMaxFlushers() {
    return indexerMaxFlushers;
  }

  public void setIndexerMaxFlushers(int indexerMaxFlushers) {
    this.indexerMaxFlushers = indexerMaxFlushers;
  }

  public String getIndexerLogRoot() {
    return indexerLogRoot;
  }

  public void setIndexerLogRoot(String indexerLogRoot) {
    this.indexerLogRoot = indexerLogRoot;
  }

  public int getIndexerMaxTransactions() {
    return indexerMaxTransactions;
  }

  public void setIndexerMaxTransactions(int indexerMaxTransactions) {
    this.indexerMaxTransactions = indexerMaxTransactions;
  }

  public int getIndexerMaxSizePerLog() {
    return indexerMaxSizePerLog;
  }

  public void setIndexerMaxSizePerLog(int indexerMaxSizePerLog) {
    this.indexerMaxSizePerLog = indexerMaxSizePerLog;
  }

  public boolean isIndexerDebuggingEnabled() {
    return indexerDebuggingEnabled;
  }

  public void setIndexerDebuggingEnabled(boolean indexerDebuggingEnabled) {
    this.indexerDebuggingEnabled = indexerDebuggingEnabled;
  }

  public int getIndexerMaxOpenedLogFiles() {
    return indexerMaxOpenedLogFiles;
  }

  public void setIndexerMaxOpenedLogFiles(int indexerMaxLogFiles) {
    this.indexerMaxOpenedLogFiles = indexerMaxLogFiles;
  }

  public boolean isEnableIndexerStrictlySyncLog() {
    return enableIndexerStrictlySyncLog;
  }

  public void setEnableIndexerStrictlySyncLog(boolean enableIndexerStrictlySyncLog) {
    this.enableIndexerStrictlySyncLog = enableIndexerStrictlySyncLog;
  }

  public int getIndexerMaxLazyFinalizations() {
    return indexerMaxLazyFinalizations;
  }

  public void setIndexerMaxLazyFinalizations(int indexerMaxLazyFinalizations) {
    this.indexerMaxLazyFinalizations = indexerMaxLazyFinalizations;
  }

  public int getIndexerMaxEntriesPerDeletion() {
    return indexerMaxEntriesPerDeletion;
  }

  public void setIndexerMaxEntriesPerDeletion(int indexerMaxEntriesPerDeletion) {
    this.indexerMaxEntriesPerDeletion = indexerMaxEntriesPerDeletion;
  }

  public long getIndexerRetryIntervalSecAfterStorageException() {
    return indexerRetryIntervalSecAfterStorageException;
  }

  public void setIndexerRetryIntervalSecAfterStorageException(
      long indexerRetryIntervalSecAfterStorageException) {
    this.indexerRetryIntervalSecAfterStorageException =
        indexerRetryIntervalSecAfterStorageException;
  }

  public int getIndexerRetryTimesAfterStorageException() {
    return indexerRetryTimesAfterStorageException;
  }

  public void setIndexerRetryTimesAfterStorageException(
      int indexerRetryTimesAfterStorageException) {
    this.indexerRetryTimesAfterStorageException = indexerRetryTimesAfterStorageException;
  }

  public int getThreadPoolSizeOfRollback() {
    return threadPoolSizeOfRollback;
  }

  public void setThreadPoolSizeOfRollback(int threadPoolSizeOfRollback) {
    this.threadPoolSizeOfRollback = threadPoolSizeOfRollback;
  }

  public String getRawDirtyPageSelectionStrategy() {
    return rawDirtyPageSelectionStrategy;
  }

  public void setRawDirtyPageSelectionStrategy(String rawDirtyPageSelectionStrategy) {
    this.rawDirtyPageSelectionStrategy = rawDirtyPageSelectionStrategy;
  }

  public String getSsdDirtyPageSelectionStrategy() {
    return ssdDirtyPageSelectionStrategy;
  }

  public void setSsdDirtyPageSelectionStrategy(String ssdDirtyPageSelectionStrategy) {
    this.ssdDirtyPageSelectionStrategy = ssdDirtyPageSelectionStrategy;
  }

  public int getPageCountInRawChunk() {
    return pageCountInRawChunk;
  }

  public void setPageCountInRawChunk(int pageCountInRawChunk) {
    this.pageCountInRawChunk = pageCountInRawChunk;
  }

  public int getMaxCopyPageNetworkIoPs() {
    return maxCopyPageNetworkIoPs;
  }

  public void setMaxCopyPageNetworkIoPs(int maxCopyPageNetworkIoPs) {
    this.maxCopyPageNetworkIoPs = maxCopyPageNetworkIoPs;
  }

  public int getMaxPersistDataToDiskIoPs() {
    return maxPersistDataToDiskIoPs;
  }

  public void setMaxPersistDataToDiskIoPs(int maxPersistDataToDiskIoPs) {
    this.maxPersistDataToDiskIoPs = maxPersistDataToDiskIoPs;
  }

  public long getThresholdToRemoveEjectedArchive() {
    return thresholdToRemoveEjectedArchive;
  }

  public void setThresholdToRemoveEjectedArchive(long thresholdToRemoveEjectedArchive) {
    this.thresholdToRemoveEjectedArchive = thresholdToRemoveEjectedArchive;
  }

  public int getMaxLogIdWindowSize() {
    return maxLogIdWindowSize;
  }

  public void setMaxLogIdWindowSize(int maxLogIdWindowSize) {
    this.maxLogIdWindowSize = maxLogIdWindowSize;
  }

  public int getMaxLogIdWindowSplitCount() {
    return maxLogIdWindowSplitCount;
  }

  public void setMaxLogIdWindowSplitCount(int maxLogIdWindowSplitCount) {
    this.maxLogIdWindowSplitCount = maxLogIdWindowSplitCount;
  }

  public int getMaxPushIntervalTimeMs() {
    return maxPushIntervalTimeMs;
  }

  public void setMaxPushIntervalTimeMs(int maxPushIntervalTimeMs) {
    this.maxPushIntervalTimeMs = maxPushIntervalTimeMs;
  }

  public int getCopyPageTimeoutMs() {
    return copyPageTimeoutMs;
  }

  public void setCopyPageTimeoutMs(int copyPageTimeoutMs) {
    this.copyPageTimeoutMs = copyPageTimeoutMs;
  }

  public int getNumberOfDirtyPagePerFlushRequestForSsd() {
    return numberOfDirtyPagePerFlushRequestForSsd;
  }

  public void setNumberOfDirtyPagePerFlushRequestForSsd(
      int numberOfDirtyPagePerFlushRequestForSsd) {
    this.numberOfDirtyPagePerFlushRequestForSsd = numberOfDirtyPagePerFlushRequestForSsd;
  }

  public int getMaxWaitForDnShutdownTimeSec() {
    return maxWaitForDnShutdownTimeSec;
  }

  public void setMaxWaitForDnShutdownTimeSec(int maxWaitForDnShutdownTimeSec) {
    this.maxWaitForDnShutdownTimeSec = maxWaitForDnShutdownTimeSec;
  }

  public int getCorePoolSizeForStateProcessingEngineExpirationChecker() {
    return corePoolSizeForStateProcessingEngineExpirationChecker;
  }

  public void setCorePoolSizeForStateProcessingEngineExpirationChecker(
      int corePoolSizeForStateProcessingEngineExpirationChecker) {
    this.corePoolSizeForStateProcessingEngineExpirationChecker =
        corePoolSizeForStateProcessingEngineExpirationChecker;
  }

  public int getMaxPoolSizeForStateProcessingEngineExpirationChecker() {
    return maxPoolSizeForStateProcessingEngineExpirationChecker;
  }

  public void setMaxPoolSizeForStateProcessingEngineExpirationChecker(
      int maxPoolSizeForStateProcessingEngineExpirationChecker) {
    this.maxPoolSizeForStateProcessingEngineExpirationChecker =
        maxPoolSizeForStateProcessingEngineExpirationChecker;
  }

  public int getReadCachePageRatio() {
    return readCachePageRatio;
  }

  public void setReadCachePageRatio(int readCachePageRatio) {
    this.readCachePageRatio = readCachePageRatio;
  }

  public String getBackupDbInfoDir() {
    return backupDbInfoDir;
  }

  public void setBackupDbInfoDir(String backupDbInfoDir) {
    this.backupDbInfoDir = backupDbInfoDir;
  }

  public String getPageSystemMemoryCacheSize() {
    return pageSystemMemoryCacheSize;
  }

  public void setPageSystemMemoryCacheSize(String pageSystemMemoryCacheSize) {
    this.pageSystemMemoryCacheSize = pageSystemMemoryCacheSize;
  }

  public long getPageSystemMemoryCacheSizeBytes() {
    return Utils.getByteSize(pageSystemMemoryCacheSize);
  }

  public int getMemoryInHeapSizeMb() {
    return memoryInHeapSizeMb;
  }

  public void setMemoryInHeapSizeMb(int memoryInHeapSizeMb) {
    this.memoryInHeapSizeMb = memoryInHeapSizeMb;
  }

  public int getMaxIoPendingRequests() {
    return maxIoPendingRequests;
  }

  public void setMaxIoPendingRequests(int maxIoPendingRequests) {
    this.maxIoPendingRequests = maxIoPendingRequests;
  }

  public int getGiveYouLogIdTimeout() {
    return giveYouLogIdTimeout;
  }

  public void setGiveYouLogIdTimeout(int giveYouLogIdTimeout) {
    this.giveYouLogIdTimeout = giveYouLogIdTimeout;
  }

  public int getNetworkHealthCheckerRateMs() {
    return networkHealthCheckerRateMs;
  }

  public void setNetworkHealthCheckerRateMs(int networkHealthCheckerRateMs) {
    this.networkHealthCheckerRateMs = networkHealthCheckerRateMs;
  }

  public boolean isEnableLoggerTracer() {
    return enableLoggerTracer;
  }

  public void setEnableLoggerTracer(boolean enableLoggerTracer) {
    this.enableLoggerTracer = enableLoggerTracer;
  }

  public int getPingHostTimeoutMs() {
    return pingHostTimeoutMs;
  }

  public void setPingHostTimeoutMs(int pingHostTimeoutMs) {
    this.pingHostTimeoutMs = pingHostTimeoutMs;
  }

  public int getNetworkConnectionDetectRetryMaxtimes() {
    return networkConnectionDetectRetryMaxtimes;
  }

  public void setNetworkConnectionDetectRetryMaxtimes(int networkConnectionDetectRetryMaxtimes) {
    this.networkConnectionDetectRetryMaxtimes = networkConnectionDetectRetryMaxtimes;
  }

  public int getNetworkConnectionCheckDelayMs() {
    return this.networkConnectionCheckDelayMs;
  }

  public void setNetworkConnectionCheckDelayMs(int networkConnectionCheckDelayMs) {
    this.networkConnectionCheckDelayMs = networkConnectionCheckDelayMs;
  }

  public int getArbiterCountLimitInOneArchive() {
    return arbiterCountLimitInOneArchive;
  }

  public void setArbiterCountLimitInOneArchive(int arbiterCountLimitInOneArchive) {
    this.arbiterCountLimitInOneArchive = arbiterCountLimitInOneArchive;
  }

  public int getFlexibleCountLimitInOneArchive() {
    return flexibleCountLimitInOneArchive;
  }

  public void setFlexibleCountLimitInOneArchive(int flexibleCountLimitInOneArchive) {
    this.flexibleCountLimitInOneArchive = flexibleCountLimitInOneArchive;
  }

  public int getArbiterCountLimitInOneDatanode() {
    return arbiterCountLimitInOneDatanode;
  }

  public void setArbiterCountLimitInOneDatanode(int arbiterCountLimitInOneDatanode) {
    this.arbiterCountLimitInOneDatanode = arbiterCountLimitInOneDatanode;
  }

  public String getIoDelayTimeunit() {
    return ioDelayTimeunit;
  }

  public void setIoDelayTimeunit(String ioDelayTimeunit) {
    this.ioDelayTimeunit = ioDelayTimeunit;
  }

  public TimeUnit getResolvedIoDelayTimeunit() {
    try {
      return TimeUnit.valueOf(StringUtils.upperCase(ioDelayTimeunit));
    } catch (IllegalArgumentException e) {
      logger.error("TimeUnit[{}] for io delay statistic in config is illegal. Use default [{}].",
          ioDelayTimeunit, TimeUnit.MILLISECONDS);
      return TimeUnit.MILLISECONDS;
    }
  }

  public int getStorageIoCheckRateSecond() {
    return storageIoCheckRateSecond;
  }

  public void setStorageIoCheckRateSecond(int storageIoCheckRateSecond) {
    this.storageIoCheckRateSecond = storageIoCheckRateSecond;
  }

  public int getConcurrentPageContextsPerPlalWorker() {
    return concurrentPageContextsPerPlalWorker;
  }

  public void setConcurrentPageContextsPerPlalWorker(int concurrentPageContextsPerPlalWorker) {
    this.concurrentPageContextsPerPlalWorker = concurrentPageContextsPerPlalWorker;
  }

  public StorageConfiguration getStorageConfiguration() {
    return storageConfiguration;
  }

  public void setStorageConfiguration(StorageConfiguration storageConfiguration) {
    this.storageConfiguration = storageConfiguration;
  }

  public int getStorageIoCountInSample() {
    return storageIoCountInSample;
  }

  public void setStorageIoCountInSample(int storageIoCountInSample) {
    this.storageIoCountInSample = storageIoCountInSample;
  }

  public int getStorageIoSampleCount() {
    return storageIoSampleCount;
  }

  public void setStorageIoSampleCount(int storageIoSampleCount) {
    this.storageIoSampleCount = storageIoSampleCount;
  }

  public int getLevelTwoFlushThrottlePageCount() {
    return levelTwoFlushThrottlePageCount;
  }

  public void setLevelTwoFlushThrottlePageCount(int levelTwoFlushThrottlePageCount) {
    this.levelTwoFlushThrottlePageCount = levelTwoFlushThrottlePageCount;
  }

  public String getIndexerDbPath() {
    return indexerDbPath;
  }

  public void setIndexerDbPath(String indexerDbPath) {
    this.indexerDbPath = indexerDbPath;
  }

  public long getIndexerWriteBufferSizePerArchive() {
    return indexerWriteBufferSizePerArchive;
  }

  public void setIndexerWriteBufferSizePerArchive(long indexerWriteBufferSizePerArchive) {
    this.indexerWriteBufferSizePerArchive = indexerWriteBufferSizePerArchive;
  }

  private CompressionType getIndexerCompressionType(String indexerCompressionType) {
    if (indexerCompressionType == null) {
      throw new IllegalArgumentException("null compression type.");
    }

    String compressionTypeStr;
    CompressionType ret;

    compressionTypeStr = indexerCompressionType.toUpperCase();
    ret = CompressionType.valueOf(compressionTypeStr);
    if (ret == null) {
      throw new IllegalArgumentException("Invalid compression type: " + indexerCompressionType);
    }

    return ret;
  }

  public CompressionType getIndexerCompressionType() {
    return getIndexerCompressionType(this.indexerCompressionType);
  }

  public void setIndexerCompressionType(String indexerCompressionType) {
    // check if the given indexer compression type is valid
    getIndexerCompressionType(indexerCompressionType);

    this.indexerCompressionType = indexerCompressionType;
  }

  public int getIndexerRetryMax() {
    return indexerRetryMax;
  }

  public void setIndexerRetryMax(int indexerRetryMax) {
    this.indexerRetryMax = indexerRetryMax;
  }

  public long getIndexerRetryIntervalMs() {
    return indexerRetryIntervalMs;
  }

  public void setIndexerRetryIntervalMs(long indexerRetryIntervalMs) {
    this.indexerRetryIntervalMs = indexerRetryIntervalMs;
  }

  public int getIndexerDeletionBatchLimit() {
    return indexerDeletionBatchLimit;
  }

  public void setIndexerDeletionBatchLimit(int indexerDeletionBatchLimit) {
    this.indexerDeletionBatchLimit = indexerDeletionBatchLimit;
  }

  public int getIndexerDeletionDelaySec() {
    return indexerDeletionDelaySec;
  }

  public void setIndexerDeletionDelaySec(int indexerDeletionDelaySec) {
    this.indexerDeletionDelaySec = indexerDeletionDelaySec;
  }

  public int getIndexerMaxBgCompactions() {
    return indexerMaxBgCompactions;
  }

  public void setIndexerMaxBgCompactions(int indexerMaxBgCompactions) {
    this.indexerMaxBgCompactions = indexerMaxBgCompactions;
  }

  public int getIndexerMaxBgFlushes() {
    return indexerMaxBgFlushes;
  }

  public void setIndexerMaxBgFlushes(int indexerMaxBgFlushes) {
    this.indexerMaxBgFlushes = indexerMaxBgFlushes;
  }

  public long getIndexerBytesPerSync() {
    return indexerBytesPerSync;
  }

  public void setIndexerBytesPerSync(long indexerBytesPerSync) {
    this.indexerBytesPerSync = indexerBytesPerSync;
  }

  public long getIndexerBlockSize() {
    return indexerBlockSize;
  }

  public void setIndexerBlockSize(long indexerBlockSize) {
    this.indexerBlockSize = indexerBlockSize;
  }

  public boolean isIndexerLevelCompactionDynamicLevelBytes() {
    return indexerLevelCompactionDynamicLevelBytes;
  }

  public void setIndexerLevelCompactionDynamicLevelBytes(
      boolean indexerLevelCompactionDynamicLevelBytes) {
    this.indexerLevelCompactionDynamicLevelBytes = indexerLevelCompactionDynamicLevelBytes;
  }

  public long getQuarantineZoneMsToRecoverDeletingSegment() {
    return quarantineZoneMsToRecoverDeletingSegment;
  }

  public void setQuarantineZoneMsToRecoverDeletingSegment(
      long quarantineZoneMsToRecoverDeletingSegment) {
    this.quarantineZoneMsToRecoverDeletingSegment = quarantineZoneMsToRecoverDeletingSegment;
  }

  public boolean isDynamicShadowPageSwitch() {
    return dynamicShadowPageSwitch;
  }

  public void setDynamicShadowPageSwitch(boolean dynamicShadowPageSwitch) {
    this.dynamicShadowPageSwitch = dynamicShadowPageSwitch;
  }

  public String getSsdCacheRocksDbPath() {
    return ssdCacheRocksDbPath;
  }

  public void setSsdCacheRocksDbPath(String ssdCacheRocksDbPath) {
    this.ssdCacheRocksDbPath = ssdCacheRocksDbPath;
  }

  public String getArchiveInitMode() {
    return archiveInitMode;
  }

  public void setArchiveInitMode(String archiveInitMode) {
    this.archiveInitMode = archiveInitMode;
  }

  public boolean isEnableFileBuffer() {
    return enableFileBuffer;
  }

  public void setEnableFileBuffer(boolean enableFileBuffer) {
    this.enableFileBuffer = enableFileBuffer;
  }

  public int getWaitFileBufferTimeOutMs() {
    return waitFileBufferTimeOutMs;
  }

  public void setWaitFileBufferTimeOutMs(int waitFileBufferTimeOutMs) {
    this.waitFileBufferTimeOutMs = waitFileBufferTimeOutMs;
  }

  public long getIndexerMaxTotalWalSize() {
    return indexerMaxTotalWalSize;
  }

  public void setIndexerMaxTotalWalSize(long indexerMaxTotalWalSize) {
    this.indexerMaxTotalWalSize = indexerMaxTotalWalSize;
  }

  public int getMaxPageCountPerPlalSchedule() {
    return maxPageCountPerPlalSchedule;
  }

  public void setMaxPageCountPerPlalSchedule(int maxPageCountPerPlalSchedule) {
    this.maxPageCountPerPlalSchedule = maxPageCountPerPlalSchedule;
  }

  public boolean isEnablePerformanceReport() {
    return enablePerformanceReport;
  }

  public void setEnablePerformanceReport(boolean enablePerformanceReport) {
    this.enablePerformanceReport = enablePerformanceReport;
  }

  public boolean isInvalidatePageManagerInPlal() {
    return invalidatePageManagerInPlal;
  }

  public void setInvalidatePageManagerInPlal(boolean invalidatePageManagerInPlal) {
    this.invalidatePageManagerInPlal = invalidatePageManagerInPlal;
  }

  public int getWriteThreadCount() {
    return writeThreadCount;
  }

  public void setWriteThreadCount(int writeThreadCount) {
    this.writeThreadCount = writeThreadCount;
  }

  public boolean isWaitForBroadcastLogId() {
    return waitForBroadcastLogId;
  }

  public void setWaitForBroadcastLogId(boolean waitForBroadcastLogId) {
    this.waitForBroadcastLogId = waitForBroadcastLogId;
  }

  public boolean isBroadcastLogId() {
    return broadcastLogId;
  }

  public void setBroadcastLogId(boolean broadcastLogId) {
    this.broadcastLogId = broadcastLogId;
  }

  public boolean isPrimaryCommitLog() {
    return primaryCommitLog;
  }

  public void setPrimaryCommitLog(boolean primaryCommitLog) {
    this.primaryCommitLog = primaryCommitLog;
  }

  public boolean isRocksDbInSsd() {
    return rocksDbInSsd;
  }

  public void setRocksDbInSsd(boolean rocksDbInSsd) {
    this.rocksDbInSsd = rocksDbInSsd;
  }

  public String getRocksDbDaultFolder() {
    return rocksDbDaultFolder;
  }

  public void setRocksDbDaultFolder(String rocksDbDaultFolder) {
    this.rocksDbDaultFolder = rocksDbDaultFolder;
  }

  public String getRocksDbPartitionFolder() {
    return rocksDbPartitionFolder;
  }

  public void setRocksDbPartitionFolder(String rocksDbPartitionFolder) {
    this.rocksDbPartitionFolder = rocksDbPartitionFolder;
  }

  public boolean isInfiniteTimeoutAllocatingFastBuffer() {
    return infiniteTimeoutAllocatingFastBuffer;
  }

  public void setInfiniteTimeoutAllocatingFastBuffer(boolean infiniteTimeoutAllocatingFastBuffer) {
    this.infiniteTimeoutAllocatingFastBuffer = infiniteTimeoutAllocatingFastBuffer;
  }

  public int getWriteObserveAreaIoPsThreshold() {
    return writeObserveAreaIoPsThreshold;
  }

  public void setWriteObserveAreaIoPsThreshold(int writeObserveAreaIoPsThreshold) {
    this.writeObserveAreaIoPsThreshold = writeObserveAreaIoPsThreshold;
  }

  public int getReadObserveAreaIoPsThreshold() {
    return readObserveAreaIoPsThreshold;
  }

  public void setReadObserveAreaIoPsThreshold(int readObserveAreaIoPsThreshold) {
    this.readObserveAreaIoPsThreshold = readObserveAreaIoPsThreshold;
  }

  public float getAvailableBucketThresholdToStartForceFlush() {
    return availableBucketThresholdToStartForceFlush;
  }

  public void setAvailableBucketThresholdToStartForceFlush(
      float availableBucketThresholdToStartForceFlush) {
    this.availableBucketThresholdToStartForceFlush = availableBucketThresholdToStartForceFlush;
  }

  public int getFlushReadMbpsThreshold() {
    return flushReadMbpsThreshold;
  }

  public void setFlushReadMbpsThreshold(int flushReadIoPsThreshold) {
    this.flushReadMbpsThreshold = flushReadIoPsThreshold;
  }

  public long getSlowDiskLatencyThresholdRandomNs() {
    return slowDiskLatencyThresholdRandomNs;
  }

  public void setSlowDiskLatencyThresholdRandomNs(long slowDiskLatencyThresholdRandomNs) {
    this.slowDiskLatencyThresholdRandomNs = slowDiskLatencyThresholdRandomNs;
  }

  public long getSlowDiskLatencyThresholdSequentialNs() {
    return slowDiskLatencyThresholdSequentialNs;
  }

  public void setSlowDiskLatencyThresholdSequentialNs(long slowDiskLatencyThresholdSequentialNs) {
    this.slowDiskLatencyThresholdSequentialNs = slowDiskLatencyThresholdSequentialNs;
  }

  public double getSlowDiskIgnoreRatio() {
    return slowDiskIgnoreRatio;
  }

  public void setSlowDiskIgnoreRatio(double slowDiskIgnoreRatio) {
    this.slowDiskIgnoreRatio = slowDiskIgnoreRatio;
  }

  public boolean isStartSlowDiskChecker() {
    return startSlowDiskChecker;
  }

  public void setStartSlowDiskChecker(boolean startSlowDiskChecker) {
    this.startSlowDiskChecker = startSlowDiskChecker;
  }

  public long getNetworkReviveThresholdMs() {
    return networkReviveThresholdMs;
  }

  public void setNetworkReviveThresholdMs(long networkReviveThresholdMs) {
    this.networkReviveThresholdMs = networkReviveThresholdMs;
  }

  public long getIoTaskThresholdMs() {
    return ioTaskThresholdMs;
  }

  public void setIoTaskThresholdMs(long ioTaskThresholdMs) {
    this.ioTaskThresholdMs = ioTaskThresholdMs;
  }

  public long getIoWriteTaskThresholdMs() {
    return ioWriteTaskThresholdMs;
  }

  public void setIoWriteTaskThresholdMs(long ioWriteTaskThresholdMs) {
    this.ioWriteTaskThresholdMs = ioWriteTaskThresholdMs;
  }

  public int getTimeoutOfCreateSegmentsMs() {
    return timeoutOfCreateSegmentsMs;
  }

  public void setTimeoutOfCreateSegmentsMs(int timeoutOfCreateSegmentsMs) {
    this.timeoutOfCreateSegmentsMs = timeoutOfCreateSegmentsMs;
  }

  public boolean isEnableIoCostWarning() {
    return enableIoCostWarning;
  }

  public void setEnableIoCostWarning(boolean enableIoCostWarning) {
    this.enableIoCostWarning = enableIoCostWarning;
  }

  public boolean isEnableLogCounter() {
    return enableLogCounter;
  }

  public void setEnableLogCounter(boolean enableLogCounter) {
    this.enableLogCounter = enableLogCounter;
  }

  public long getReadPageTimeoutMs() {
    return readPageTimeoutMs;
  }

  public void setReadPageTimeoutMs(long readPageTimeoutMs) {
    this.readPageTimeoutMs = readPageTimeoutMs;
  }

  public boolean isDisableSystemOutput() {
    return disableSystemOutput;
  }

  public void setDisableSystemOutput(boolean disableSystemOutput) {
    this.disableSystemOutput = disableSystemOutput;
  }

  public int getLogsCountWarningThreshold() {
    return logsCountWarningThreshold;
  }

  public void setLogsCountWarningThreshold(int logsCountWarningThreshold) {
    this.logsCountWarningThreshold = logsCountWarningThreshold;
  }

  public int getMaxNumberLogsForSecondaryToSyncUpOnSecondarySide() {
    return maxNumberLogsForSecondaryToSyncUpOnSecondarySide;
  }

  public void setMaxNumberLogsForSecondaryToSyncUpOnSecondarySide(
      int maxNumberLogsForSecondaryToSyncUpOnSecondarySide) {
    this.maxNumberLogsForSecondaryToSyncUpOnSecondarySide =
        maxNumberLogsForSecondaryToSyncUpOnSecondarySide;
  }

  public int getLogsCountWarningLevel() {
    return logsCountWarningLevel;
  }

  public void setLogsCountWarningLevel(int logsCountWarningLevel) {
    this.logsCountWarningLevel = logsCountWarningLevel;
  }

  public int getPageSystemCount() {
    return pageSystemCount;
  }

  public void setPageSystemCount(int pageSystemCount) {
    this.pageSystemCount = pageSystemCount;
  }

  public int getCatchUpLogEnginesCount() {
    return catchUpLogEnginesCount;
  }

  public void setCatchUpLogEnginesCount(int catchUpLogEnginesCount) {
    this.catchUpLogEnginesCount = catchUpLogEnginesCount;
  }

  public int getStateProcessingEnginesCount() {
    return stateProcessingEnginesCount;
  }

  public void setStateProcessingEnginesCount(int stateProcessingEnginesCount) {
    this.stateProcessingEnginesCount = stateProcessingEnginesCount;
  }

  public int getMinThreadPoolSizeForSegmentUnitTaskExecutors() {
    return minThreadPoolSizeForSegmentUnitTaskExecutors;
  }

  public void setMinThreadPoolSizeForSegmentUnitTaskExecutors(
      int minThreadPoolSizeForSegmentUnitTaskExecutors) {
    this.minThreadPoolSizeForSegmentUnitTaskExecutors =
        minThreadPoolSizeForSegmentUnitTaskExecutors;
  }

  public String getArchiveIdRecordPath() {
    return archiveIdRecordPath;
  }

  public void setArchiveIdRecordPath(String archiveIdRecordPath) {
    this.archiveIdRecordPath = archiveIdRecordPath;
  }

  public long getAlarmReportRateMs() {
    return alarmReportRateMs;
  }

  public void setAlarmReportRateMs(long alarmReportRateMs) {
    this.alarmReportRateMs = alarmReportRateMs;
  }

  public boolean isEnableIoTracing() {
    return enableIoTracing;
  }

  public void setEnableIoTracing(boolean enableIoTracing) {
    this.enableIoTracing = enableIoTracing;
  }

  public boolean isEnableFrontIoCostWarning() {
    return enableFrontIoCostWarning;
  }

  public void setEnableFrontIoCostWarning(boolean enableFrontIoCostWarning) {
    this.enableFrontIoCostWarning = enableFrontIoCostWarning;
  }

  public long getFrontIoTaskThresholdMs() {
    return frontIoTaskThresholdMs;
  }

  public void setFrontIoTaskThresholdMs(long frontIoTaskThresholdMs) {
    this.frontIoTaskThresholdMs = frontIoTaskThresholdMs;
  }

  public boolean isArchiveInitSlowDiskCheckerEnabled() {
    return archiveInitSlowDiskCheckerEnabled;
  }

  public int getArchiveInitDiskThresholdSequential() {
    return archiveInitDiskThresholdSequential;
  }

  public void setArchiveInitDiskThresholdSequential(int archiveInitDiskThresholdSequential) {
    this.archiveInitDiskThresholdSequential = archiveInitDiskThresholdSequential;
  }

  public int getArchiveInitDiskThresholdRandom() {
    return archiveInitDiskThresholdRandom;
  }

  public void setArchiveInitDiskThresholdRandom(int archiveInitDiskThresholdRandom) {
    this.archiveInitDiskThresholdRandom = archiveInitDiskThresholdRandom;
  }

  public long getSecondaryFirstVotingTimeoutMs() {
    return secondaryFirstVotingTimeoutMs;
  }

  public void setSecondaryFirstVotingTimeoutMs(long secondaryFirstVotingTimeoutMs) {
    this.secondaryFirstVotingTimeoutMs = secondaryFirstVotingTimeoutMs;
  }

  public boolean isEnableCleanDbAfterFlushedBucket() {
    return enableCleanDbAfterFlushedBucket;
  }

  public void setEnableCleanDbAfterFlushedBucket(boolean enableCleanDbAfterFlushedBucket) {
    this.enableCleanDbAfterFlushedBucket = enableCleanDbAfterFlushedBucket;
  }

  @Override
  public String toString() {
    return "DataNodeConfiguration{"
        + "storageConfiguration=" + storageConfiguration
        + ", nettyConfiguration=" + nettyConfiguration
        + ", enablePageAddressValidation=" + enablePageAddressValidation
        + ", rocksDbInSSd=" + rocksDbInSsd
        + ", rocksDBDaultFolder='" + rocksDbDaultFolder + '\''
        + ", rocksDBPartitionFolder='" + rocksDbPartitionFolder + '\''
        + ", arbiterStoreFile='" + arbiterStoreFile + '\''
        + ", archiveConfiguration=" + archiveConfiguration
        + ", brokenDiskStorageExceptionThreshold=" + brokenDiskStorageExceptionThreshold
        + ", numberOfPages=" + numberOfPages
        + ", infiniteTimeoutAllocatingFastBuffer=" + infiniteTimeoutAllocatingFastBuffer
        + ", startSlowDiskChecker=" + startSlowDiskChecker
        + ", memorySizeForDataLogsMB=" + memorySizeForDataLogsMb
        + ", startFlusher=" + startFlusher
        + ", defaultPageRequestTimeoutMS=" + defaultPageRequestTimeoutMs
        + ", flushPagesIntervalMS=" + flushPagesIntervalMs
        + ", numberOfPageCorrectors=" + numberOfPageCorrectors
        + ", secondaryLeaseInPrimaryMS=" + secondaryLeaseInPrimaryMs
        + ", secondaryLeaseMS=" + secondaryLeaseMs
        + ", newBornSecondaryLeaseMS=" + newBornSecondaryLeaseMs
        + ", extensionRequestTimeOutMS=" + extensionRequestTimeOutMs
        + ", extensionConnectionTimeoutMs=" + extensionConnectionTimeoutMs
        + ", otherStatusesLeaseMS=" + otherStatusesLeaseMs
        + ", inteverlForCheckPageReleasedMs=" + inteverlForCheckPageReleasedMs
        + ", rateOfReportingArchivesMS=" + rateOfReportingArchivesMs
        + ", archiveReservedRatio=" + archiveReservedRatio
        + ", archiveUsedSpaceCalculateDelayMS=" + archiveUsedSpaceCalculateDelayMs
        + ", rateOfReportingSegmentsMS=" + rateOfReportingSegmentsMs
        + ", timeoutOfCreateSegmentsMS=" + timeoutOfCreateSegmentsMs
        + ", dataNodeRequestTimeoutMS=" + dataNodeRequestTimeoutMs
        + ", requestNewSegmentUnitExpirationThresholdMS="
        + requestNewSegmentUnitExpirationThresholdMs
        + ", rateOfExtendingLeaseMS=" + rateOfExtendingLeaseMs
        + ", rateOfSegmentUnitDeleterExecutionMS=" + rateOfSegmentUnitDeleterExecutionMs
        + ", waitTimeMSToMoveToDeleted=" + waitTimeMsToMoveToDeleted
        + ", waitTimeMSFromBrokenToDeleted=" + waitTimeMsFromBrokenToDeleted
        + ", needCheckRollbackInOffling=" + needCheckRollbackInOffling
        + ", quarantineZoneMSToRecoverDeletingSegment=" + quarantineZoneMsToRecoverDeletingSegment
        + ", rateOfDuplicatingVolumeMetadataJsonMS=" + rateOfDuplicatingVolumeMetadataJsonMs
        + ", rateOfJanitorExecutionMS=" + rateOfJanitorExecutionMs
        + ", maxNumberLogsForSecondaryToSyncUp=" + maxNumberLogsForSecondaryToSyncUp
        + ", maxNumberLogsForSecondaryToSyncUpOnSecondarySide="
        + maxNumberLogsForSecondaryToSyncUpOnSecondarySide
        + ", numberOfCopyPagePerStorage=" + numberOfCopyPagePerStorage
        + ", maxNumberOfMissLogsPerSync=" + maxNumberOfMissLogsPerSync
        + ", giveMeYourLogsCountEachTime=" + giveMeYourLogsCountEachTime
        + ", maxNumberOfPagesToApplyPerDrive=" + maxNumberOfPagesToApplyPerDrive
        + ", maxNumberOfLogsToPersistPerDrive=" + maxNumberOfLogsToPersistPerDrive
        + ", probabilityToSyncLogsWhenNoEnoughLogs=" + probabilityToSyncLogsWhenNoEnoughLogs
        + ", coreOfCleanUpTimeoutLogsExecturoThreads=" + coreOfCleanUpTimeoutLogsExecturoThreads
        + ", maxOfCleanUpTimeoutLogsExecturoThreads=" + maxOfCleanUpTimeoutLogsExecturoThreads
        + ", periodCleanUpTimeoutLogsPerArchive=" + periodCleanUpTimeoutLogsPerArchive
        + ", timeoutOfCleanUpCompletingLogsMs=" + timeoutOfCleanUpCompletingLogsMs
        + ", timeoutOfCleanUpCrashOutUuidMs=" + timeoutOfCleanUpCrashOutUuidMs
        + ", thresholdToRequestForNewMemberMS=" + thresholdToRequestForNewMemberMs
        + ", thresholdOfMissingPagesToRequestForNewMember="
        + thresholdOfMissingPagesToRequestForNewMember
        + ", thresholdOfMissingLogsToRequestForNewMember="
        + thresholdOfMissingLogsToRequestForNewMember
        + ", thresholdToClearSecondaryPlMS=" + thresholdToClearSecondaryPlMs
        + ", thresholdToClearSecondaryClMS=" + thresholdToClearSecondaryClMs
        + ", thresholdToRemoveEjectedArchive=" + thresholdToRemoveEjectedArchive
        + ", maxConnectionsPerAsyncDataNodeEndPoint=" + maxConnectionsPerAsyncDataNodeEndPoint
        + ", maxConnectionsPerSyncDataNodeEndPoint=" + maxConnectionsPerSyncDataNodeEndPoint
        + ", minNumWorkerThreads=" + minNumWorkerThreads
        + ", maxNumWorkerThreads=" + maxNumWorkerThreads
        + ", corePoolSizeForCatchupLogEnginePCL=" + corePoolSizeForCatchupLogEnginePcl
        + ", maxPoolSizeForCatchupLogEnginePCL=" + maxPoolSizeForCatchupLogEnginePcl
        + ", corePoolSizeForCatchupLogEnginePPL=" + corePoolSizeForCatchupLogEnginePpl
        + ", maxPoolSizeForCatchupLogEnginePPL=" + maxPoolSizeForCatchupLogEnginePpl
        + ", corePoolSizeForStateProcessingEngineOther="
        + corePoolSizeForStateProcessingEngineOther
        + ", maxPoolSizeForStateProcessingEngineOther=" + maxPoolSizeForStateProcessingEngineOther
        + ", corePoolSizeForStateProcessingEngineExpirationChecker="
        + corePoolSizeForStateProcessingEngineExpirationChecker
        + ", maxPoolSizeForStateProcessingEngineExpirationChecker="
        + +maxPoolSizeForStateProcessingEngineExpirationChecker
        + ", corePoolSizeForStateProcessingEngineHeartbeat="
        + corePoolSizeForStateProcessingEngineHeartbeat
        + ", maxPoolSizeForStateProcessingEngineHeartbeat="
        + maxPoolSizeForStateProcessingEngineHeartbeat
        + ", threadPoolSizeOfRollback=" + threadPoolSizeOfRollback
        + ", rateOfShadowPageDeleterInMs=" + rateOfShadowPageDeleterInMs
        + ", maxChannelPendingSizeMb=" + maxChannelPendingSizeMb
        + ", storageCheckRateMS=" + storageCheckRateMs
        + ", persistSegUnitMetadataRateMS=" + persistSegUnitMetadataRateMs
        + ", writeStorageIOStep=" + writeStorageIoStep
        + ", readStorageIOStep=" + readStorageIoStep
        + ", maxIODataSizePerRequestBytes=" + maxIoDataSizePerRequestBytes
        + ", maxIoPendingRequests=" + maxIoPendingRequests
        + ", storageIOCheckRateSecond=" + storageIoCheckRateSecond
        + ", storageIOCountInSample=" + storageIoCountInSample
        + ", storageIOSampleCount=" + storageIoSampleCount
        + ", maxNumberOfPLALWorkerPendingPage=" + maxNumberOfPlalWorkerPendingPage
        + ", concurrentPageContextsPerPLALWorker=" + concurrentPageContextsPerPlalWorker
        + ", maxThreadpoolSizePerStorage=" + maxThreadpoolSizePerStorage
        + ", maxIODepthPerHDDStorage=" + maxIoDepthPerHddStorage
        + ", maxIODepthPerSSDStorage=" + maxIoDepthPerSsdStorage
        + ", numberOfDirtyPagePerFlushRequest=" + numberOfDirtyPagePerFlushRequest
        + ", waitForMemoryBufferTimeoutMs=" + waitForMemoryBufferTimeoutMs
        + ", maxSynchronizeTimeForCreateLogMs=" + maxSynchronizeTimeForCreateLogMs
        + ", fastBufferAllocateAligned=" + fastBufferAllocateAligned
        + ", numberOfBuffersForMissingLog=" + numberOfBuffersForMissingLog
        + ", primaryFastBufferPercentage=" + primaryFastBufferPercentage
        + ", secondaryFastBufferPercentage=" + secondaryFastBufferPercentage
        + ", syncLogFastBufferPercentage=" + syncLogFastBufferPercentage
        + ", fastBufferAlignmentBytes=" + fastBufferAlignmentBytes
        + ", rawDirtyPageSelectionStrategy='" + rawDirtyPageSelectionStrategy + '\''
        + ", indexerDBPath='" + indexerDbPath + '\''
        + ", indexerMaxBGCompactions=" + indexerMaxBgCompactions
        + ", indexerMaxBGFlushes=" + indexerMaxBgFlushes
        + ", indexerBytesPerSync=" + indexerBytesPerSync
        + ", indexerBlockSize=" + indexerBlockSize
        + ", indexerWriteBufferSizePerArchive=" + indexerWriteBufferSizePerArchive
        + ", indexerCompressionType='" + indexerCompressionType + '\''
        + ", indexerRetryMax=" + indexerRetryMax
        + ", indexerRetryIntervalMS=" + indexerRetryIntervalMs
        + ", indexerEnablePinL0FilterAndIndexBlocksInCache="
        + ", indexerLevelCompactionDynamicLevelBytes=" + indexerLevelCompactionDynamicLevelBytes
        + ", indexerMaxTotalWalSize=" + indexerMaxTotalWalSize
        + ", indexerDeletionBatchLimit=" + indexerDeletionBatchLimit
        + ", indexerDeletionDelaySec=" + indexerDeletionDelaySec
        + ", indexerMaxPersistLogNumberWaitOnceSync=" + indexerMaxPersistLogNumberWaitOnceSync
        + ", indexerPersistLease=" + indexerPersistLease
        + ", indexerEnableTracing=" + indexerEnableTracing
        + ", indexerTracingLevel=" + indexerTracingLevel
        + ", indexerDiscardPositiveHistoryPeriod=" + indexerDiscardPositiveHistoryPeriod
        + ", indexerRocksdbLogLevel='" + indexerRocksdbLogLevel + '\''
        + ", indexerRocksdbMaxLogFileSize=" + indexerRocksdbMaxLogFileSize
        + ", indexerRocksdbKeepLogFileNumber=" + indexerRocksdbKeepLogFileNumber
        + ", indexerMaxTransactions=" + indexerMaxTransactions
        + ", indexerMaxFlushers=" + indexerMaxFlushers
        + ", indexerFlusherBundleSize=" + indexerFlusherBundleSize
        + ", indexerFlusherBundleTimeoutSec=" + indexerFlusherBundleTimeoutSec
        + ", indexerLogRoot='" + indexerLogRoot + '\''
        + ", indexerMaxSizePerLog=" + indexerMaxSizePerLog
        + ", indexerDebuggingEnabled=" + indexerDebuggingEnabled
        + ", indexerMaxOpenedLogFiles=" + indexerMaxOpenedLogFiles
        + ", indexerMaxLazyFinalizations=" + indexerMaxLazyFinalizations
        + ", indexerMaxEntriesPerDeletion=" + indexerMaxEntriesPerDeletion
        + ", enableIndexerStrictlySyncLog=" + enableIndexerStrictlySyncLog
        + ", indexerRetryIntervalSecAfterStorageException="
        + indexerRetryIntervalSecAfterStorageException
        + ", indexerRetryTimesAfterStorageException=" + indexerRetryTimesAfterStorageException
        + ", pageCountInRawChunk=" + pageCountInRawChunk
        + ", maxCopyPageNetworkIOPS=" + maxCopyPageNetworkIoPs
        + ", maxCopyPageStorageIOPS=" + maxCopyPageStorageIoPs
        + ", maxPersistDataTODiskIOPS=" + maxPersistDataToDiskIoPs
        + ", maxPushIntervalTimeMs=" + maxPushIntervalTimeMs
        + ", maxLogIdWindowSize=" + maxLogIdWindowSize
        + ", maxLogIdWindowSplitCount=" + maxLogIdWindowSplitCount
        + ", shutdownSaveLogDir='" + shutdownSaveLogDir + '\''
        + ", backupDBInfoDir='" + backupDbInfoDir + '\''
        + ", maxWaitForDNShutdownTimeSec=" + maxWaitForDnShutdownTimeSec
        + ", copyPageTimeoutMs=" + copyPageTimeoutMs
        + ", ssdDirtyPageSelectionStrategy='" + ssdDirtyPageSelectionStrategy + '\''
        + ", maxThreadpoolSizePerSSD=" + maxThreadpoolSizePerSsd
        + ", flushWaitMS=" + flushWaitMs
        + ", maxNumberOfPLALWorkerPendingPageForSsd=" + maxNumberOfPlalWorkerPendingPageForSsd
        + ", numberOfDirtyPagePerFlushRequestForSsd=" + numberOfDirtyPagePerFlushRequestForSsd
        + ", readCachePageRatio=" + readCachePageRatio
        + ", pageChecksumAlgorithm='" + pageChecksumAlgorithm + '\''
        + ", networkChecksumAlgorithm='" + networkChecksumAlgorithm + '\''
        + ", pageSystemMemoryCacheSize='" + pageSystemMemoryCacheSize + '\''
        + ", dynamicShadowPageSwitch=" + dynamicShadowPageSwitch
        + ", memoryInHeapSizeMB=" + memoryInHeapSizeMb
        + ", timeoutWaitForSecondaryLogComing=" + timeoutWaitForSecondaryLogComing
        + ", giveYouLogIdTimeout=" + giveYouLogIdTimeout
        + ", logDirs='" + logDirs + '\''
        + ", minRequiredLogSizeMb=" + minRequiredLogSizeMb
        + ", minReservedDiskSizeMb=" + minReservedDiskSizeMb
        + ", networkHealthCheckerRateMS=" + networkHealthCheckerRateMs
        + ", networkReviveThresholdMS=" + networkReviveThresholdMs
        + ", delayRecordStorageExceptionMs=" + delayRecordStorageExceptionMs
        + ", targetIoLatencyInDataNodeMs=" + targetIoLatencyInDataNodeMs
        + ", pingHostTimeoutMs=" + pingHostTimeoutMs
        + ", networkConnectionDetectRetryMaxtimes=" + networkConnectionDetectRetryMaxtimes
        + ", networkConnectionCheckDelayMs=" + networkConnectionCheckDelayMs
        + ", networkConnectionDetectServerListeningPort="
        + networkConnectionDetectServerListeningPort
        + ", networkConnectionDetectServerUsingNativeOrNot="
        + networkConnectionDetectServerUsingNativeOrNot
        + ", cacheStorageIOSequentialCondition=" + cacheStorageIoSequentialCondition
        + ", needRetryBroadCastWhenBecomePrimary=" + needRetryBroadCastWhenBecomePrimary
        + ", storageIOTimeout=" + storageIoTimeout
        + ", syncLogTaskExecutorThreads=" + syncLogTaskExecutorThreads
        + ", syncLogPackageFrameSize=" + syncLogPackageFrameSize
        + ", backwardSyncLogPackageFrameSize=" + backwardSyncLogPackageFrameSize
        + ", syncLogPackageResponseWaitTimeMs=" + syncLogPackageResponseWaitTimeMs
        + ", syncLogPackageWaitTimeMs=" + syncLogPackageWaitTimeMs
        + ", syncLogMaxWaitProcessTimeMs=" + syncLogMaxWaitProcessTimeMs
        + ", swlogIdUsingMinLogIdOfSencondarys=" + swlogIdUsingMinLogIdOfSencondarys
        + ", ssdCacheRocksDBPath='" + ssdCacheRocksDbPath + '\''
        + ", ssdCacheEnablePinL0FilterAndIndexBlocksInCache="
        + ", brickPreAllocatorUpperThreshold=" + brickPreAllocatorUpperThreshold
        + ", brickPreAllocatorLowerThreshold=" + brickPreAllocatorLowerThreshold
        + ", secondaryPCLDriverTimeoutMS=" + secondaryPclDriverTimeoutMs
        + ", secondarySyncLogTimeoutMS=" + secondarySyncLogTimeoutMs
        + ", rocksDbPathConfig=" + rocksDbPathConfig
        + ", startSlowDiskAwaitChecker=" + startSlowDiskAwaitChecker
        + ", iostatFileName='" + iostatFileName + '\''
        + ", slowDiskAwaitThreshold=" + slowDiskAwaitThreshold
        + ", slowDiskAwaitCheckInterval=" + slowDiskAwaitCheckInterval
        + ", slowDiskLatencyThresholdRandomNS=" + slowDiskLatencyThresholdRandomNs
        + ", slowDiskLatencyThresholdSequentialNS=" + slowDiskLatencyThresholdSequentialNs
        + ", archiveInitSlowDiskCheckerEnabled=" + archiveInitSlowDiskCheckerEnabled
        + ", archiveInitDiskThresholdSequential=" + archiveInitDiskThresholdSequential
        + ", archiveInitDiskThresholdRandom=" + archiveInitDiskThresholdRandom
        + ", slowDiskIgnoreRatio=" + slowDiskIgnoreRatio
        + ", slowDiskStrategy=" + slowDiskStrategy
        + ", enableMergeReadOnTimeout=" + enableMergeReadOnTimeout
        + ", archiveInitMode='" + archiveInitMode + '\''
        + ", enableFileBuffer=" + enableFileBuffer
        + ", waitFileBufferTimeOutMs=" + waitFileBufferTimeOutMs
        + ", maxPageCountPerPLALSchedule=" + maxPageCountPerPlalSchedule
        + ", enablePerformanceReport=" + enablePerformanceReport
        + ", invalidatePageManagerInPLAL=" + invalidatePageManagerInPlal
        + ", writeThreadCount=" + writeThreadCount
        + ", rawAppName='" + rawAppName + '\''
        + ", writeTokenCount=" + writeTokenCount
        + ", waitForBroadcastLogId=" + waitForBroadcastLogId
        + ", broadcastLogId=" + broadcastLogId
        + ", primaryCommitLog=" + primaryCommitLog
        + ", becomePrimaryThreadPoolSize=" + becomePrimaryThreadPoolSize
        + ", arbiterCountLimitInOneArchive=" + arbiterCountLimitInOneArchive
        + ", flexibleCountLimitInOneArchive=" + flexibleCountLimitInOneArchive
        + ", arbiterCountLimitInOneDatanode=" + arbiterCountLimitInOneDatanode
        + ", concurrentCopyPageTaskCount=" + concurrentCopyPageTaskCount
        + ", smartMaxCopySpeedMb=" + smartMaxCopySpeedMb
        + ", ioDelayTimeunit='" + ioDelayTimeunit + '\''
        + ", maxPageCountPerPush=" + maxPageCountPerPush
        + ", levelTwoFlushThrottlePageCount=" + levelTwoFlushThrottlePageCount
        + ", writeObserveAreaIOPSThreshold=" + writeObserveAreaIoPsThreshold
        + ", readObserveAreaIOPSThreshold=" + readObserveAreaIoPsThreshold
        + ", availableBucketThresholdToStartForceFlush="
        + availableBucketThresholdToStartForceFlush
        + ", flushReadMBPSThreshold=" + flushReadMbpsThreshold
        + ", IOTaskThresholdMS=" + ioTaskThresholdMs
        + ", IOWriteTaskThresholdMS=" + ioWriteTaskThresholdMs
        + ", enableIOCostWarning=" + enableIoCostWarning
        + ", enableLogCounter=" + enableLogCounter
        + ", pageMetadataSize=" + pageMetadataSize
        + ", pageMetadataNeedFlushToDisk=" + pageMetadataNeedFlushToDisk
        + ", readPageTimeoutMS=" + readPageTimeoutMs
        + ", disableSystemOutput=" + disableSystemOutput
        + ", logsCountWarningThreshold=" + logsCountWarningThreshold
        + ", logsCountWarningLevel=" + logsCountWarningLevel
        + ", pageSystemCount=" + pageSystemCount
        + ", catchUpLogEnginesCount=" + catchUpLogEnginesCount
        + ", stateProcessingEnginesCount=" + stateProcessingEnginesCount
        + ", minThreadPoolSizeForSegmentUnitTaskExecutors="
        + minThreadPoolSizeForSegmentUnitTaskExecutors
        + ", enableIoTracing=" + enableIoTracing
        + ", enableFrontIOCostWarning=" + enableFrontIoCostWarning
        + ", frontIOTaskThresholdMS=" + frontIoTaskThresholdMs
        + ", alarmReportRateMS=" + alarmReportRateMs
        + ", secondaryFirstVotingTimeoutMS=" + secondaryFirstVotingTimeoutMs
        + ", enableCleanDBAfterFlushedBucket=" + enableCleanDbAfterFlushedBucket
        + ", skippingReadDoubleCheck=" + skippingReadDoubleCheck
        + ", systemExitOnPersistLogError=" + systemExitOnPersistLogError
        + ", forceFullCopy=" + forceFullCopy
        + ", enableLoggerTracer=" + enableLoggerTracer
        + ", archiveIdRecordPath='" + archiveIdRecordPath + '\''
        + ", blockVotingWhenMigratingSecondaryHasMaxPcl="
        + blockVotingWhenMigratingSecondaryHasMaxPcl
        + ", arbiterSyncPersistMembershipOnIoPath=" + arbiterSyncPersistMembershipOnIoPath
        + '}';
  }

  public int getIndexerMaxPersistLogNumberWaitOnceSync() {
    return indexerMaxPersistLogNumberWaitOnceSync;
  }

  public void setIndexerMaxPersistLogNumberWaitOnceSync(
      int indexerMaxPersistLogNumberWaitOnceSync) {
    this.indexerMaxPersistLogNumberWaitOnceSync = indexerMaxPersistLogNumberWaitOnceSync;
  }

  public int getIndexerPersistLease() {
    return indexerPersistLease;
  }

  public void setIndexerPersistLease(int indexerPersistLease) {
    this.indexerPersistLease = indexerPersistLease;
  }

  public void setIndexerEnableTracing(boolean indexerEnableTracing) {
    this.indexerEnableTracing = indexerEnableTracing;
  }

  public int getIndexerDiscardPositiveHistoryPeriod() {
    return indexerDiscardPositiveHistoryPeriod;
  }

  public void setIndexerDiscardPositiveHistoryPeriod(int indexerDiscardPositiveHistoryPeriod) {
    this.indexerDiscardPositiveHistoryPeriod = indexerDiscardPositiveHistoryPeriod;
  }

  public boolean isSkippingReadDoubleCheck() {
    return skippingReadDoubleCheck;
  }

  public void setSkippingReadDoubleCheck(boolean skippingReadDoubleCheck) {
    this.skippingReadDoubleCheck = skippingReadDoubleCheck;
  }

  public boolean isEnableMergeReadOnTimeout() {
    return enableMergeReadOnTimeout;
  }

  public void setEnableMergeReadOnTimeout(boolean enableMergeReadOnTimeout) {
    this.enableMergeReadOnTimeout = enableMergeReadOnTimeout;
  }

  public int getBrickPreAllocatorUpperThreshold() {
    return brickPreAllocatorUpperThreshold;
  }

  public void setBrickPreAllocatorUpperThreshold(int brickPreAllocatorUpperThreshold) {
    this.brickPreAllocatorUpperThreshold = brickPreAllocatorUpperThreshold;
  }

  public int getBrickPreAllocatorLowerThreshold() {
    return brickPreAllocatorLowerThreshold;
  }

  public void setBrickPreAllocatorLowerThreshold(int brickPreAllocatorLowerThreshold) {
    this.brickPreAllocatorLowerThreshold = brickPreAllocatorLowerThreshold;
  }

  public int getArchiveUsedSpaceCalculateDelayMs() {
    return archiveUsedSpaceCalculateDelayMs;
  }

  public void setArchiveUsedSpaceCalculateDelayMs(int archiveUsedSpaceCalculateDelayMs) {
    this.archiveUsedSpaceCalculateDelayMs = archiveUsedSpaceCalculateDelayMs;
  }

  public int getCoreOfCleanUpTimeoutLogsExecturoThreads() {
    return coreOfCleanUpTimeoutLogsExecturoThreads;
  }

  public void setCoreOfCleanUpTimeoutLogsExecturoThreads(
      int coreOfCleanUpTimeoutLogsExecturoThreads) {
    this.coreOfCleanUpTimeoutLogsExecturoThreads = coreOfCleanUpTimeoutLogsExecturoThreads;
  }

  public int getMaxOfCleanUpTimeoutLogsExecturoThreads() {
    return maxOfCleanUpTimeoutLogsExecturoThreads;
  }

  public void setMaxOfCleanUpTimeoutLogsExecturoThreads(
      int maxOfCleanUpTimeoutLogsExecturoThreads) {
    this.maxOfCleanUpTimeoutLogsExecturoThreads = maxOfCleanUpTimeoutLogsExecturoThreads;
  }

  public int getPeriodCleanUpTimeoutLogsPerArchive() {
    return periodCleanUpTimeoutLogsPerArchive;
  }

  public void setPeriodCleanUpTimeoutLogsPerArchive(int periodCleanUpTimeoutLogsPerArchive) {
    this.periodCleanUpTimeoutLogsPerArchive = periodCleanUpTimeoutLogsPerArchive;
  }

  public int getTimeoutOfCleanUpCompletingLogsMs() {
    return timeoutOfCleanUpCompletingLogsMs;
  }

  public void setTimeoutOfCleanUpCompletingLogsMs(int timeoutOfCleanUpCompletingLogsMs) {
    this.timeoutOfCleanUpCompletingLogsMs = timeoutOfCleanUpCompletingLogsMs;
  }

  public int getTimeoutOfCleanUpCrashOutUuidMs() {
    return timeoutOfCleanUpCrashOutUuidMs;
  }

  public void setTimeoutOfCleanUpCrashOutUuidMs(int timeoutOfCleanUpCrashOutUuidMs) {
    this.timeoutOfCleanUpCrashOutUuidMs = timeoutOfCleanUpCrashOutUuidMs;
  }

  public boolean isSystemExitOnPersistLogError() {
    return systemExitOnPersistLogError;
  }

  public void setSystemExitOnPersistLogError(boolean systemExitOnPersistLogError) {
    this.systemExitOnPersistLogError = systemExitOnPersistLogError;
  }

  public int getIndexerTracingLevel() {
    return indexerTracingLevel;
  }

  public void setIndexerTracingLevel(int indexerTracingLevel) {
    this.indexerTracingLevel = indexerTracingLevel;
  }

  public boolean isEnablePageAddressValidation() {
    return enablePageAddressValidation;
  }

  public void setEnablePageAddressValidation(boolean enablePageAddressValidation) {
    this.enablePageAddressValidation = enablePageAddressValidation;
  }

  public NettyConfiguration getNettyConfiguration() {
    return nettyConfiguration;
  }

  public String getIndexerRocksdbLogLevel() {
    return indexerRocksdbLogLevel;
  }

  public void setIndexerRocksdbLogLevel(String indexerRocksdbLogLevel) {
    this.indexerRocksdbLogLevel = indexerRocksdbLogLevel;
  }

  public int getIndexerRocksdbMaxLogFileSize() {
    return indexerRocksdbMaxLogFileSize;
  }

  public void setIndexerRocksdbMaxLogFileSize(int indexerRocksdbMaxLogFileSize) {
    this.indexerRocksdbMaxLogFileSize = indexerRocksdbMaxLogFileSize;
  }

  public int getIndexerRocksdbKeepLogFileNumber() {
    return indexerRocksdbKeepLogFileNumber;
  }

  public void setIndexerRocksdbKeepLogFileNumber(int indexerRocksdbKeepLogFileNumber) {
    this.indexerRocksdbKeepLogFileNumber = indexerRocksdbKeepLogFileNumber;
  }

  public boolean isBlockVotingWhenMigratingSecondaryHasMaxPcl() {
    return blockVotingWhenMigratingSecondaryHasMaxPcl;
  }

  public void setBlockVotingWhenMigratingSecondaryHasMaxPcl(
      boolean blockVotingWhenMigratingSecondaryHasMaxPcl) {
    this.blockVotingWhenMigratingSecondaryHasMaxPcl = blockVotingWhenMigratingSecondaryHasMaxPcl;
  }

  public boolean isArbiterSyncPersistMembershipOnIoPath() {
    return arbiterSyncPersistMembershipOnIoPath;
  }

  public void setArbiterSyncPersistMembershipOnIoPath(
      boolean arbiterSyncPersistMembershipOnIoPath) {
    this.arbiterSyncPersistMembershipOnIoPath = arbiterSyncPersistMembershipOnIoPath;
  }

  public int getMaxGapOfPclLoggingForSyncLogs() {
    return maxGapOfPclLoggingForSyncLogs;
  }

  public void setMaxGapOfPclLoggingForSyncLogs(int maxGapOfPclLoggingForSyncLogs) {
    this.maxGapOfPclLoggingForSyncLogs = maxGapOfPclLoggingForSyncLogs;
  }

}
