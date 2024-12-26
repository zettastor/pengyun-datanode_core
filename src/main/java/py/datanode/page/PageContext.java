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

package py.datanode.page;

import py.archive.page.PageAddress;
import py.archive.segment.SegId;
import py.engine.Latency;
import py.storage.Storage;

public interface PageContext<P extends Page> {
  P getPage();

  void setPage(P page);

  void waitFor() throws InterruptedException;

  void done();

  PageAddress getPageAddressForIo();

  TaskType getTaskType();

  void setTaskType(TaskType taskType);

  boolean isSuccess();

  Exception getCause();

  void setCause(Exception e);

  IoType getIoType();

  void setIoType(IoType ioType);

  void setExpiredTime(long expiredTime);

  boolean isExpired();

  PageContext<P> getOriginalPageContext();

  Storage getStorage();

  void cancel();

  boolean isCancel();

  Latency getLatency();

  void setLatency(Latency latency);

  default void updateSegId(SegId segId) {
    getPage().getAddress().setSegId(segId);
  }
}
