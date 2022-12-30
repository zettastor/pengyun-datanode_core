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
