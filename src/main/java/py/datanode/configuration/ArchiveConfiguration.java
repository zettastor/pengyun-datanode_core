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
@PropertySource("classpath:config/archive.properties")
public class ArchiveConfiguration {
  @Value("${archive.plugin.matcher:plugin rawName %s, archiveType %s}")
  private String archivePluginMatcher = "plugin rawName %s, archiveType %s";

  @Value("${archive.plugout.matcher:plugout devName %s, serialNumber %s}")
  private String archivePlugoutMatcher = "plugout devName %s, serialNumber %s";

  //the raw dev prepare be deleted,for example: ejected devList raw1
  @Value("${archive.ejected.matcher:ejected rawName %s, serialNumber %s}")
  private String archiveEjectedMatcher = "ejected rawName %s, serialNumber %s";

  @Value("${check.archive.script.path:bin/CheckArchives.sh}")
  private String checkArchiveScriptPath = "bin/CheckArchives.sh";

  @Value("${move.archive.script.path:bin/ReinitArchive.sh}")
  private String moveArchiveScriptPath = "bin/ReinitArchive.sh";

  @Value("${plug.Out.archive.script.path:bin/plugout.sh}")
  private String plugOutArchiveScriptPath = "bin/plugout.sh";

  @Value("${check.archive.ejected.script.path:bin/CheckPlugOutRawDisks.pl}")
  private String checkStoragePlugoutScriptPath = "bin/CheckPlugOutRawDisks.pl";

  @Value("${relink.unsettle.disk.script.path:bin/relink_unsettle_disk.py}")
  private String relinkUnsettleDiskScriptPath = "bin/relink_unsettle_disk.py";

  // sub directory of persistence root where we write disk logs
  private String diskLogDir = "disklog";

  @Value("${snapshot.log.directory:var/storage/snapshot}")
  private String snapshotLogDir = "var/storage/snapshot";

  @Value("${persist.root.directory:var/storage}")
  private String persistRootDir = "var/storage";

  @Value("${data.archive.directory:rawDisks}")
  private String dataArchiveDir = "rawDisks";

  @Value("${unset.archive.directory:unsetDisks}")
  private String unsettledArchiveDir = "unsetDisks";

  @Value("${file.buffer.path:var/storage/filebuffer}")
  private String fileBufferPath = "var/storage/filebuffer";

  @Value("${file.buffer.size.gb:6}")
  private int fileBufferSizeGb;

  @Value("${file.buffer.observation.duration:3000}")
  private int fileBufferObservationDuration;

  @Value("${file.buffer.rejection.percent:30}")
  private int fileBufferRejectionPercent = 30;

  @Value("${on.vms:false}")
  private boolean onVms = false;

  public ArchiveConfiguration() {
  }

  public String getMoveArchiveScriptPath() {
    return moveArchiveScriptPath;
  }

  public String getArchivePluginMatcher() {
    return archivePluginMatcher;
  }

  public void setArchivePluginMatcher(String archivePluginMatcher) {
    this.archivePluginMatcher = archivePluginMatcher;
  }

  public String getArchivePlugoutMatcher() {
    return archivePlugoutMatcher;
  }

  public void setArchivePlugoutMatcher(String archivePlugoutMatcher) {
    this.archivePlugoutMatcher = archivePlugoutMatcher;
  }

  public String getArchiveEjectedMatcher() {
    return archiveEjectedMatcher;
  }

  public void setArchiveEjectedMatcher(String archiveEjectedMatcher) {
    this.archiveEjectedMatcher = archiveEjectedMatcher;
  }

  public String getCheckArchiveScriptPath() {
    return checkArchiveScriptPath;
  }

  public void setCheckArchiveScriptPath(String checkArchiveScriptPath) {
    this.checkArchiveScriptPath = checkArchiveScriptPath;
  }

  public String getPlugOutArchiveScriptPath() {
    return plugOutArchiveScriptPath;
  }

  public void setPlugOutArchiveScriptPath(String plugOutArchiveScriptPath) {
    this.plugOutArchiveScriptPath = plugOutArchiveScriptPath;
  }

  public String getCheckStoragePlugoutScriptPath() {
    return checkStoragePlugoutScriptPath;
  }

  public void setCheckStoragePlugoutScriptPath(String checkStoragePlugoutScriptPath) {
    this.checkStoragePlugoutScriptPath = checkStoragePlugoutScriptPath;
  }

  public String getDiskLogDir() {
    return diskLogDir;
  }

  public void setDiskLogDir(String diskLogDir) {
    this.diskLogDir = diskLogDir;
  }

  public String getPersistRootDir() {
    return persistRootDir;
  }

  public void setPersistRootDir(String persistRootDir) {
    this.persistRootDir = persistRootDir;
  }

  public String getDataArchiveDir() {
    return dataArchiveDir;
  }

  public void setDataArchiveDir(String dataArchiveDir) {
    this.dataArchiveDir = dataArchiveDir;
  }

  public String getFileBufferPath() {
    return fileBufferPath;
  }

  public void setFileBufferPath(String fileBufferPath) {
    this.fileBufferPath = fileBufferPath;
  }

  public int getFileBufferSizeGb() {
    return fileBufferSizeGb;
  }

  public void setFileBufferSizeGb(int fileBufferSizeGb) {
    this.fileBufferSizeGb = fileBufferSizeGb;
  }

  public int getFileBufferObservationDuration() {
    return fileBufferObservationDuration;
  }

  public void setFileBufferObservationDuration(int fileBufferObservationDuration) {
    this.fileBufferObservationDuration = fileBufferObservationDuration;
  }

  public int getFileBufferRejectionPercent() {
    return fileBufferRejectionPercent;
  }

  public void setFileBufferRejectionPercent(int fileBufferRejectionPercent) {
    this.fileBufferRejectionPercent = fileBufferRejectionPercent;
  }

  public boolean isOnVms() {
    return onVms;
  }

  public void setOnVms(boolean onVms) {
    this.onVms = onVms;
  }

  public String getUnsettledArchiveDir() {
    return unsettledArchiveDir;
  }

  public void setUnsettledArchiveDir(String unsettledArchiveDir) {
    this.unsettledArchiveDir = unsettledArchiveDir;
  }

  public String getRelinkUnsettleDiskScriptPath() {
    return relinkUnsettleDiskScriptPath;
  }

  public void setRelinkUnsettleDiskScriptPath(String relinkUnsettleDiskScriptPath) {
    this.relinkUnsettleDiskScriptPath = relinkUnsettleDiskScriptPath;
  }

  public String getSnapshotLogDir() {
    return snapshotLogDir;
  }

  public void setSnapshotLogDir(String snapshotLogDir) {
    this.snapshotLogDir = snapshotLogDir;
  }

  @Override
  public String toString() {
    return "ArchiveConfiguration{" + "archivePluginMatcher='" + archivePluginMatcher + '\''
        + ", archivePlugoutMatcher='" + archivePlugoutMatcher + '\'' + ", archiveEjectedMatcher='"
        + archiveEjectedMatcher + '\'' + ", checkArchiveScriptPath='" + checkArchiveScriptPath
        + '\''
        + ", plugOutArchiveScriptPath='" + plugOutArchiveScriptPath + '\''
        + ", checkStoragePlugoutScriptPath='"
        + checkStoragePlugoutScriptPath + '\'' + ", diskLogDir='" + diskLogDir + '\''
        + ", persistRootDir='"
        + ", fileBufferPath='" + fileBufferPath + '\'' + ", fileBufferSizeGB=" + fileBufferSizeGb
        + ", fileBufferObservationDuration=" + fileBufferObservationDuration
        + ", fileBufferRejectionPercent="
        + fileBufferRejectionPercent + ", onVMs=" + onVms
        + ", relinkUnsettleDiskScriptPath=" + relinkUnsettleDiskScriptPath
        + '}';
  }
}
