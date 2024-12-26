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

package py.datanode.statistic;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import py.archive.Archive;
import py.archive.ArchiveStatus;
import py.common.PyService;
import py.monitor.common.CounterName;
import py.monitor.common.OperationName;
import py.monitor.common.UserDefineName;

public class AlarmReportData {
  private final PyService pyService;
  private final OperationName operationName;
  private final Map<CounterName, Long> counters;
  private final Map<UserDefineName, String> userDefineParams;

  public AlarmReportData(PyService pyService, OperationName operationName,
      Map<CounterName, Long> counters, Map<UserDefineName, String> userDefineParams) {
    this.pyService = pyService;
    this.operationName = operationName;
    this.counters = counters;
    this.userDefineParams = userDefineParams;
  }

  public static AlarmReportData generateIoDelayAlarm(long delay, TimeUnit timeUnit) {
    Map<CounterName, Long> counters = new HashMap<>(1);
    counters.put(CounterName.DATANODE_IO_DELAY, delay);

    Map<UserDefineName, String> userDefineParams = new HashMap<>(1);
    userDefineParams.put(UserDefineName.TimeUnit, timeUnit.name());

    return new AlarmReportData(PyService.DATANODE, OperationName.DatanodeIo, counters,
        userDefineParams);
  }

  public static AlarmReportData generateArchiveStatusAlarm(Archive archive, ArchiveStatus oldStatus,
      ArchiveStatus newStatus) {
    long archiveId = archive.getArchiveMetadata().getArchiveId();
    Map<CounterName, Long> counters = new HashMap<>(1);
    counters.put(CounterName.DISK_STATUS, 0L);

    Map<UserDefineName, String> userDefineParams = new HashMap<>(4);
    userDefineParams.put(UserDefineName.ArchiveID, String.valueOf(archiveId));
    userDefineParams.put(UserDefineName.ArchiveOldStatus, oldStatus.name());
    userDefineParams.put(UserDefineName.ArchiveNewStatus, newStatus.name());
    userDefineParams.put(UserDefineName.ArchiveName, archive.getArchiveMetadata().getDeviceName());

    return new AlarmReportData(PyService.DATANODE, OperationName.Disk, counters, userDefineParams);
  }

  public static AlarmReportData generateQueueSizeAlarm(long length, String queueName) {
    Map<CounterName, Long> counters = new HashMap<>(1);
    counters.put(CounterName.DATANODE_REQUEST_QUEUE_LENGTH, length);

    Map<UserDefineName, String> userDefineParams = new HashMap<>(1);
    userDefineParams.put(UserDefineName.QueueName, queueName);

    return new AlarmReportData(PyService.DATANODE, OperationName.DataNode, counters,
        userDefineParams);
  }

  public PyService getPyService() {
    return pyService;
  }

  public OperationName getOperationName() {
    return operationName;
  }

  public Map<String, Long> getCounters() {
    return counters.entrySet().stream()
        .collect(Collectors.toMap(entry -> entry.getKey().name(), Map.Entry::getValue));
  }

  public Map<String, String> getUserDefineParams() {
    return userDefineParams.entrySet().stream()
        .collect(Collectors.toMap(entry -> entry.getKey().name(), Map.Entry::getValue));
  }

  @Override
  public String toString() {
    return "AlarmReportData{" 
        + "pyService=" + pyService 
        + ", operationName=" + operationName 
        + ", counters=" + counters 
        + ", userDefineParams=" + userDefineParams 
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof AlarmReportData)) {
      return false;
    }

    AlarmReportData that = (AlarmReportData) o;

    if (pyService != that.pyService) {
      return false;
    }
    if (operationName != that.operationName) {
      return false;
    }
    if (counters != null ? !counters.equals(that.counters) : that.counters != null) {
      return false;
    }
    return userDefineParams != null ? userDefineParams.equals(that.userDefineParams)
        : that.userDefineParams == null;
  }

  @Override
  public int hashCode() {
    int result = pyService != null ? pyService.hashCode() : 0;
    result = 31 * result + (operationName != null ? operationName.hashCode() : 0);
    result = 31 * result + (counters != null ? counters.hashCode() : 0);
    result = 31 * result + (userDefineParams != null ? userDefineParams.hashCode() : 0);
    return result;
  }
}
