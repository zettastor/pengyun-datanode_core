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

import py.archive.page.PageAddress;
import py.datanode.page.Page;
import py.function.Callback;

public interface StorageIoWorker {
  void submitRead(Callback callback, PageAddress pageAddress, Page page, boolean external);

  void submitWrite(Callback callback, Page page, boolean external);

  /**
   * get last external IO time.
   */
  long lastIoTime();

  /**
   * get the overall pending request count.
   */
  int pendingRequestCount();

  void notifyPendingWork();

  void start();

  void stop();
}
