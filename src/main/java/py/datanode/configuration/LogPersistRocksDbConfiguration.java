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

import org.rocksdb.CompressionType;
import org.rocksdb.InfoLogLevel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:config/datanode.properties")
public class LogPersistRocksDbConfiguration {
  @Value("${log.persist.rocksdb.floder:rocks_db_log_entry}")
  private String dbDirName = "rocks_db_log_entry";

  @Value("${num.logs.of.one.segmentunit:100000}")
  private int maxNumLogsOfOneSegmentunit = 50000;

  @Value("${num.segmentunits.in.rocksdb:125}")
  private int maxNumSegmentunitsInRocksdb = 125;

  @Value("${log.persist.rocksdb.sync.flag:false}")
  private boolean rocksdbSyncFlag = false;

  @Value("${num.logs.cache.rocksdb.async:100}")
  private int maxNumCacheAsync = 100;

  @Value("${flag.logs.cache.using.rocksdb:false}")
  private boolean logsCacheUsingRocksDbFlag = false;
  /**
   * two types of serializations are supported now. One is compact and the other is json format. The
   * default one is compact.
   */
  @Value("${log.serialization.type:compact}")
  private String logSerializationType = "compact";

  @Value("${log.max.background.compactions:4}")
  private int logMaxBgCompactions = 4;

  @Value("${log.max.background.flushes:2}")
  private int logMaxBgFlushes = 2;

  @Value("${log.bytes.per.sync:1048576}")
  private long logBytesPerSync = 1048576;

  @Value("${log.block.size:16384}")
  private long logBlockSize = 16384;

  @Value("${log.block.cache.size.per.column:134217728}")
  private long logBlockCacheSizePerColumn = 128L * 1024 * 1024;

  @Value("${log.write.buffer.size.per.column:67108864}")
  private long logWriteBufferSizePerColumn = 67108864L;

  // 10G
  @Value("${log.max.total.wal.size:1073741824}")
  private long logMaxTotalWalSize = 10737418240L;

  @Value("${log.enable.cache.index.and.filter.blocks:true}")
  private boolean logEnableCacheIndexAndFilterBlocks = true;

  @Value("${log.enable.pin.l0.filter.and.index.blocks.in.cache:true}")
  private boolean logEnablePinL0FilterAndIndexBlocksInCache = true;

  @Value("${log.level.compaction.dynamic.level.bytes:true}")
  private boolean logLevelCompactionDynamicLevelBytes = true;

  @Value("${log.compression.type:no_compression}")
  private String logCompressionType = CompressionType.SNAPPY_COMPRESSION.name();

  @Value("${log.rocksdb.log.level:WARN_LEVEL}")
  private String rocksdbLogLevel = InfoLogLevel.WARN_LEVEL.name();
  // 1G
  @Value("${log.max.log.file.size:1073741824}")
  private int maxLogFileSize = 1073741824;
  @Value("${log.keep.log.file.number:5}")
  private int keepLogFileNumber = 5;

  private RocksDbPathConfig rocksDbPathConfig = null;

  private String logPersistRockDbUsedDir = null;
  private String logPersistRockDbDefaultDir = null;

  public LogPersistRocksDbConfiguration() {
  }

  public LogPersistRocksDbConfiguration(LogPersistRocksDbConfiguration cfg) {
    this.dbDirName = cfg.dbDirName;
    this.maxNumLogsOfOneSegmentunit = cfg.maxNumLogsOfOneSegmentunit;
    this.maxNumSegmentunitsInRocksdb = cfg.maxNumSegmentunitsInRocksdb;

    this.rocksdbSyncFlag = cfg.rocksdbSyncFlag;
    this.maxNumCacheAsync = cfg.maxNumCacheAsync;
    this.logsCacheUsingRocksDbFlag = cfg.logsCacheUsingRocksDbFlag;
    this.logSerializationType = cfg.logSerializationType;
    this.logMaxBgCompactions = cfg.logMaxBgCompactions;
    this.logMaxBgFlushes = cfg.logMaxBgFlushes;
    this.logBytesPerSync = cfg.logBytesPerSync;
    this.logBlockSize = cfg.logBlockSize;
    this.logBlockCacheSizePerColumn = cfg.logBlockCacheSizePerColumn;
    this.logWriteBufferSizePerColumn = cfg.logWriteBufferSizePerColumn;
    this.logMaxTotalWalSize = cfg.logMaxTotalWalSize;

    this.logEnableCacheIndexAndFilterBlocks = cfg.logEnableCacheIndexAndFilterBlocks;
    this.logEnablePinL0FilterAndIndexBlocksInCache = cfg.logEnablePinL0FilterAndIndexBlocksInCache;
    this.logLevelCompactionDynamicLevelBytes = cfg.logLevelCompactionDynamicLevelBytes;
    this.logCompressionType = cfg.logCompressionType;
    this.logPersistRockDbUsedDir = cfg.logPersistRockDbUsedDir;
    this.logPersistRockDbDefaultDir = cfg.logPersistRockDbDefaultDir;
    this.rocksDbPathConfig = cfg.rocksDbPathConfig;
    this.maxLogFileSize = cfg.maxLogFileSize;
    this.keepLogFileNumber = cfg.keepLogFileNumber;
  }

  public RocksDbPathConfig getRocksDbPathConfig() {
    return rocksDbPathConfig;
  }

  public void setRocksDbPathConfig(RocksDbPathConfig rocksDbPathConfig) {
    this.rocksDbPathConfig = rocksDbPathConfig;
  }

  public String getDbDirName() {
    return dbDirName;
  }

  public void setDbDirName(String dbDirName) {
    this.dbDirName = dbDirName;
  }

  public int getMaxNumLogsOfOneSegmentunit() {
    return this.maxNumLogsOfOneSegmentunit;
  }

  public void setMaxNumLogsOfOneSegmentunit(int maxNumLogsOfOneSegmentunit) {
    this.maxNumLogsOfOneSegmentunit = maxNumLogsOfOneSegmentunit;
  }

  public int getMaxNumSegmentunitsInRocksdb() {
    return this.maxNumSegmentunitsInRocksdb;
  }

  public void setMaxNumSegmentunitsInRocksdb(int maxNumSegmentunitsInRocksdb) {
    this.maxNumSegmentunitsInRocksdb = maxNumLogsOfOneSegmentunit;
  }

  public String getLogSerializationType() {
    return logSerializationType;
  }

  public void setLogSerializationType(String logSerializationType) {
    this.logSerializationType = logSerializationType;
  }

  public boolean getRocksDbSyncFlag() {
    return rocksdbSyncFlag;
  }

  public void setRocksdbSyncFlag(boolean rocksdbSyncFlag) {
    this.rocksdbSyncFlag = rocksdbSyncFlag;
  }

  public int getMaxNumCacheAsync() {
    return maxNumCacheAsync;
  }

  public void setMaxNumCacheAsync(int maxNumCacheAsync) {
    this.maxNumCacheAsync = maxNumCacheAsync;
  }

  public boolean getLogsCacheUsingRocksDbFlag() {
    return logsCacheUsingRocksDbFlag;
  }

  public void setLogsCacheUsingRocksDbFlag(boolean logsCacheUsingRocksDbFlag) {
    this.logsCacheUsingRocksDbFlag = logsCacheUsingRocksDbFlag;
  }

  public int getLogMaxBgCompactions() {
    return logMaxBgCompactions;
  }

  public void setLogMaxBgCompactions(int logMaxBgCompactions) {
    this.logMaxBgCompactions = logMaxBgCompactions;
  }

  public int getLogMaxBgFlushes() {
    return logMaxBgFlushes;
  }

  public void setLogMaxBgFlushes(int logMaxBgFlushes) {
    this.logMaxBgFlushes = logMaxBgFlushes;
  }

  public long getLogBytesPerSync() {
    return logBytesPerSync;
  }

  public void setLogBytesPerSync(long logBytesPerSync) {
    this.logBytesPerSync = logBytesPerSync;
  }

  public long getLogBlockSize() {
    return logBlockSize;
  }

  public void setLogBlockSize(long logBlockSize) {
    this.logBlockSize = logBlockSize;
  }

  public long getLogBlockCacheSizePerColumn() {
    return logBlockCacheSizePerColumn;
  }

  public void setLogBlockCacheSizePerColumn(long logBlockCacheSizePerColumn) {
    this.logBlockCacheSizePerColumn = logBlockCacheSizePerColumn;
  }

  public long getLogMaxTotalWalSize() {
    return logMaxTotalWalSize;
  }

  public void setLogMaxTotalWalSize(long logMaxTotalWalSize) {
    this.logMaxTotalWalSize = logMaxTotalWalSize;
  }

  public long getLogWriteBufferSizePerColumn() {
    return logWriteBufferSizePerColumn;
  }

  public void setLogWriteBufferSizePerColumn(long logWriteBufferSizePerColumn) {
    this.logWriteBufferSizePerColumn = logWriteBufferSizePerColumn;
  }

  public boolean isLogEnableCacheIndexAndFilterBlocks() {
    return logEnableCacheIndexAndFilterBlocks;
  }

  public void setLogEnableCacheIndexAndFilterBlocks(boolean logEnableCacheIndexAndFilterBlocks) {
    this.logEnableCacheIndexAndFilterBlocks = logEnableCacheIndexAndFilterBlocks;
  }

  public boolean isLogEnablePinL0FilterAndIndexBlocksInCache() {
    return logEnablePinL0FilterAndIndexBlocksInCache;
  }

  public void setLogEnablePinL0FilterAndIndexBlocksInCache(
      boolean logEnablePinL0FilterAndIndexBlocksInCache) {
    this.logEnablePinL0FilterAndIndexBlocksInCache = logEnablePinL0FilterAndIndexBlocksInCache;
  }

  public boolean isLogLevelCompactionDynamicLevelBytes() {
    return logLevelCompactionDynamicLevelBytes;
  }

  public void setLogLevelCompactionDynamicLevelBytes(boolean logLevelCompactionDynamicLevelBytes) {
    this.logLevelCompactionDynamicLevelBytes = logLevelCompactionDynamicLevelBytes;
  }

  public String getLogCompressionType() {
    return logCompressionType;
  }

  public void setLogCompressionType(String logCompressionType) {
    this.logCompressionType = logCompressionType;
  }

  public String getLogPersistRockDbUsedDir() {
    return logPersistRockDbUsedDir;
  }

  public void setLogPersistRockDbUsedDir(String logPersistRockDbUsedDir) {
    this.logPersistRockDbUsedDir = logPersistRockDbUsedDir;
  }

  public String getLogPersistRockDbDefaultDir() {
    return logPersistRockDbDefaultDir;
  }

  public void setLogPersistRockDbDefaultDir(String logPersistRockDbDefaultDir) {
    this.logPersistRockDbDefaultDir = logPersistRockDbDefaultDir;
  }

  public int getMaxLogFileSize() {
    return maxLogFileSize;
  }

  public void setMaxLogFileSize(int maxLogFileSize) {
    this.maxLogFileSize = maxLogFileSize;
  }

  public int getKeepLogFileNumber() {
    return keepLogFileNumber;
  }

  public void setKeepLogFileNumber(int keepLogFileNumber) {
    this.keepLogFileNumber = keepLogFileNumber;
  }

  public String getRocksdbLogLevel() {
    return rocksdbLogLevel;
  }

  public void setRocksdbLogLevel(String rocksdbLogLevel) {
    this.rocksdbLogLevel = rocksdbLogLevel;
  }

  @Override
  public String toString() {
    return "LogPersistRocksDBConfiguration [dbDirName=" + dbDirName
        + ", maxNumLogsOfOneSegmentunit=" + maxNumLogsOfOneSegmentunit
        + ", maxNumSegmentunitsInRocksdb=" + maxNumSegmentunitsInRocksdb + "," 
        + " logSerializationType=" + logSerializationType + "]";
  }

}
