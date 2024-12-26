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

package py.datanode.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:config/datanode.properties")
public class LogPersistingConfiguration {
  @Value("${log.persist.root.dir:/tmp/pengyun_logs}")
  private String rootDirName = "/tmp/pengyun_logs";

  @Value("${num.logs.in.one.file:50000}")
  private int maxNumLogsInOneFile = 50000;

  @Value("${num.files.in.datanode:50000}")
  private int maxNumFilesInDataNode = 50000;

  @Value("${num.files.in.one.segment.unit:400}")
  private int maxNumFilesInOneSegmentUnit = 400;

  /**
   * two types of serializations are supported now. One is compact and the other is json format. The
   * default one is compact.
   */
  @Value("${log.serialization.type:compact}")
  private String logSerializationType = "compact";

  public LogPersistingConfiguration() {
  }

  public LogPersistingConfiguration(LogPersistingConfiguration cfg) {
    this.rootDirName = cfg.rootDirName;
    this.maxNumLogsInOneFile = cfg.maxNumLogsInOneFile;
    this.maxNumFilesInDataNode = cfg.maxNumFilesInDataNode;
  }

  public int getMaxNumLogsInOneFile() {
    return maxNumLogsInOneFile;
  }

  public void setMaxNumLogsInOneFile(int maxNumLogsInOneFile) {
    this.maxNumLogsInOneFile = maxNumLogsInOneFile;
  }

  public String getRootDirName() {
    return rootDirName;
  }

  public void setRootDirName(String rootDirName) {
    this.rootDirName = rootDirName;
  }

  public String getLogSerializationType() {
    return logSerializationType;
  }

  public void setLogSerializationType(String logSerializationType) {
    this.logSerializationType = logSerializationType;
  }

  public int getMaxNumFilesInDataNode() {
    return maxNumFilesInDataNode;
  }

  public void setMaxNumFilesInDataNode(int maxNumFilesInDataNode) {
    this.maxNumFilesInDataNode = maxNumFilesInDataNode;
  }

  public int getMaxNumFilesInOneSegmentUnit() {
    return maxNumFilesInOneSegmentUnit;
  }

  public void setMaxNumFilesInOneSegmentUnit(int maxNumFilesInOneSegmentUnit) {
    this.maxNumFilesInOneSegmentUnit = maxNumFilesInOneSegmentUnit;
  }

  @Override
  public String toString() {
    return "LogPersistingConfiguration [rootDirName=" + rootDirName + ", maxNumLogsInOneFile="
        + maxNumLogsInOneFile
        + ", maxNumFilesInDataNode=" + maxNumFilesInDataNode + ", maxNumFilesInOneSegmentUnit="
        + maxNumFilesInOneSegmentUnit + ", logSerializationType=" + logSerializationType + "]";
  }
}
