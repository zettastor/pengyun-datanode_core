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

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RocksDbPathConfig {
  private static final Logger logger = LoggerFactory.getLogger(RocksDbPathConfig.class);
  private DataNodeConfiguration dataNodeConfiguration;

  //rocksDB Path
  private String logPersistRocksDbPath = null;
  private String logPersistRocksDbDefaultPath = null;
  //indexer rocksDB Path
  private String indexerRocksDbPath = null;
  private String indexerRocksDbDefaultPath = null;

  private String wcRocksDbPath = null;
  private String wcRocksDbDefaulPath = null;

  private File partitionDir = null;

  private LogPersisterPathType logPersisterPathType = 
      LogPersisterPathType.LOG_PERSISTER_PATH_USING_GENERATE;
  private WcRocksDbPathType wcRocksDbPathType = WcRocksDbPathType.WC_ROCKS_DB_PATH_USING_GENERATE;

  public RocksDbPathConfig(DataNodeConfiguration dataNodeCfg,
      LogPersistRocksDbConfiguration logPersistRocksDbConfiguration) {
    dataNodeConfiguration = dataNodeCfg;
    File root = new File(dataNodeCfg.getArchiveConfiguration().getPersistRootDir());
    partitionDir = new File(root, dataNodeCfg.getRocksDbPartitionFolder());

    File defaultDir = new File(partitionDir, dataNodeCfg.getRocksDbDaultFolder());
    if (!defaultDir.exists()) {
      try {
        FileUtils.forceMkdir(defaultDir);
      } catch (IOException e) {
        logger.error("can not create");
        e.printStackTrace();
      }
    }
    Validate.isTrue(defaultDir.exists());
    File logPersistRocksDbDefaultFile = new File(defaultDir,
        logPersistRocksDbConfiguration.getDbDirName());
    logPersistRocksDbDefaultPath = logPersistRocksDbDefaultFile.getAbsolutePath();
    indexerRocksDbDefaultPath = new File(defaultDir, dataNodeCfg.getIndexerDbPath())
        .getAbsolutePath();
    wcRocksDbDefaulPath = new File(defaultDir, dataNodeCfg.getSsdCacheRocksDbPath())
        .getAbsolutePath();

    logger.warn("the default is log{}, wc{}, indexer{}", logPersistRocksDbDefaultPath,
        wcRocksDbDefaulPath,
        indexerRocksDbDefaultPath);
    if (logPersistRocksDbDefaultFile.exists()) {
     
      logPersistRocksDbPath = logPersistRocksDbDefaultPath;
      indexerRocksDbPath = indexerRocksDbDefaultPath;
      logger.warn("the use dir is log{},index{}", logPersistRocksDbPath, indexerRocksDbPath);
      return;
    }

    File selectSsdFile = null;

    for (File file : partitionDir.listFiles()) {
      if (!file.isDirectory()) {
        continue;
      }
      if (StringUtils.contains(file.getAbsolutePath(), dataNodeCfg.getRocksDbDaultFolder())) {
        continue;
      }

      File logPersistDir = new File(file, logPersistRocksDbConfiguration.getDbDirName());
      if (logPersistDir.exists()) {
       
        logPersistRocksDbPath = logPersistDir.getAbsolutePath();
        indexerRocksDbPath = new File(file, dataNodeCfg.getIndexerDbPath()).getAbsolutePath();
        wcRocksDbPath = new File(file, dataNodeCfg.getSsdCacheRocksDbPath()).getPath();

        logger.warn("the use dir is log{},index{},wc {}", logPersistRocksDbPath, indexerRocksDbPath,
            wcRocksDbDefaulPath);
        return;
      }

      selectSsdFile = file;
    }

    if (selectSsdFile == null) {
     
      logPersistRocksDbPath = logPersistRocksDbDefaultPath;
      indexerRocksDbPath = indexerRocksDbDefaultPath;
      logger.warn("the use dir is log{},index{}", logPersistRocksDbPath, indexerRocksDbPath);
      return;
    }

    logPersistRocksDbPath = new File(selectSsdFile, logPersistRocksDbConfiguration.getDbDirName())
        .getAbsolutePath();
    indexerRocksDbPath = new File(selectSsdFile, dataNodeCfg.getIndexerDbPath()).getAbsolutePath();
    wcRocksDbPath = new File(selectSsdFile, dataNodeCfg.getSsdCacheRocksDbPath()).getAbsolutePath();
    logger.warn("the use dir is log{},index{},wc {}", logPersistRocksDbPath, indexerRocksDbPath,
        wcRocksDbDefaulPath);
  }

  public RocksDbPathConfig(RocksDbPathConfig rhs) {
    this.dataNodeConfiguration = rhs.dataNodeConfiguration;
    this.logPersistRocksDbPath = rhs.logPersistRocksDbPath;
    this.logPersistRocksDbDefaultPath = rhs.logPersistRocksDbDefaultPath;
    this.indexerRocksDbPath = rhs.indexerRocksDbPath;
    this.indexerRocksDbDefaultPath = rhs.indexerRocksDbDefaultPath;
    this.wcRocksDbPath = rhs.wcRocksDbPath;
    this.wcRocksDbDefaulPath = rhs.wcRocksDbDefaulPath;
    this.partitionDir = rhs.partitionDir;
  }

  public LogPersisterPathType getLogPersisterPathType() {
    return logPersisterPathType;
  }

  public void setLogPersisterPathType(LogPersisterPathType logPersisterPathType) {
    this.logPersisterPathType = logPersisterPathType;
  }

  public String getLogPersistRocksDbPath() {
    if (this.logPersisterPathType == LogPersisterPathType.LOG_PERSISTER_PATH_USING_GENERATE
        && dataNodeConfiguration
        .isRocksDbInSsd()) {
      return logPersistRocksDbPath;
    } else {
      return logPersistRocksDbDefaultPath;
    }
  }

  public void setLogPersistRocksDbPath(String logPersistRocksDbPath) {
    this.logPersistRocksDbPath = logPersistRocksDbPath;
  }

  public String getLogPersistRocksDbDefaultPath() {
    return logPersistRocksDbDefaultPath;
  }

  public void setLogPersistRocksDbDefaultPath(String logPersistRocksDbDefaultPath) {
    this.logPersistRocksDbDefaultPath = logPersistRocksDbDefaultPath;
  }

  public String getIndexerRocksDbPath() {
    if (dataNodeConfiguration.isRocksDbInSsd()) {
      return indexerRocksDbPath;
    } else {
      return indexerRocksDbDefaultPath;
    }
  }

  public void setIndexerRocksDbPath(String indexerRocksDbPath) {
    this.indexerRocksDbPath = indexerRocksDbPath;
  }

  public String getIndexerRocksDbDefaultPath() {
    return indexerRocksDbDefaultPath;
  }

  public void setIndexerRocksDbDefaultPath(String indexerRocksDbDefaultPath) {
    this.indexerRocksDbDefaultPath = indexerRocksDbDefaultPath;
  }

  public WcRocksDbPathType getWcRocksDbPathType() {
    return wcRocksDbPathType;
  }

  public void setWcRocksDbPathType(WcRocksDbPathType wcRocksDbPathType) {
    this.wcRocksDbPathType = wcRocksDbPathType;
  }

  public String getWcRocksDbPath() {
    if (this.wcRocksDbPathType == WcRocksDbPathType.WC_ROCKS_DB_PATH_USING_GENERATE
        && dataNodeConfiguration
        .isRocksDbInSsd()) {
      return this.wcRocksDbPath;
    } else {
      return this.wcRocksDbDefaulPath;
    }
  }

  public void setWcRocksDbPath(String wcRocksDbPath) {
    this.wcRocksDbPath = wcRocksDbPath;
  }

  public String getWcRocksDbDefaulPath() {
    return wcRocksDbDefaulPath;
  }

  public void setWcRocksDbDefaulPath(String wcRocksDbDefaulPath) {
    this.wcRocksDbDefaulPath = wcRocksDbDefaulPath;
  }

  public void setwcRocksDbPathByPartitionName(String partitionName) {
    File file = new File(partitionDir, partitionName);
    if (!file.exists()) {
      return;
    }
    wcRocksDbPath = new File(file, dataNodeConfiguration.getSsdCacheRocksDbPath())
        .getAbsolutePath();
    logger.warn("set wc rocks DB path {}", wcRocksDbPath);

  }

  public File getPartitionDir() {
    return partitionDir;
  }

  public void setPartitionDir(String partitionDir) {
    this.partitionDir = new File(partitionDir);
  }

  public enum LogPersisterPathType {
    LOG_PERSISTER_PATH_USING_DEFAULT,
    LOG_PERSISTER_PATH_USING_GENERATE,
  }

  public enum WcRocksDbPathType {
    WC_ROCKS_DB_PATH_USING_DEFAULT,
    WC_ROCKS_DB_PATH_USING_GENERATE,
  }
}
