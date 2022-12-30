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
