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

package py.datanode.page.context;

import java.util.concurrent.CountDownLatch;
import py.archive.page.PageAddress;
import py.datanode.page.Page;
import py.datanode.page.TaskType;
import py.engine.BogusLatency;
import py.engine.Latency;

public class PageFlushContextImpl<T extends Page> extends AbstractPageContext<T> {
  private final PageAddress pageAddressToFlush;
  private final TaskType taskType;
  private final CountDownLatch latch;
  private Exception exception;
  private Latency latency = BogusLatency.DEFAULT;

  public PageFlushContextImpl(TaskType taskType, PageAddress pageAddressToFlush,
      CountDownLatch latch) {
    this.taskType = taskType;
    this.pageAddressToFlush = pageAddressToFlush;
    this.latch = latch;
  }

  @Override
  public PageAddress getPageAddressForIo() {
    return pageAddressToFlush;
  }

  @Override
  public TaskType getTaskType() {
    return taskType;
  }

  @Override
  public Latency getLatency() {
    return latency;
  }

  @Override
  public void setLatency(Latency latency) {
    this.latency = latency;
  }

  @Override
  public Exception getCause() {
    return exception;
  }

  @Override
  public void setCause(Exception e) {
    this.exception = e;
  }

  @Override
  public boolean isSuccess() {
    return exception == null;
  }

  @Override
  public void done() {
    if (latch != null) {
      latch.countDown();
    }
  }

  @Override
  public void waitFor() throws InterruptedException {
    if (latch != null) {
      latch.await();
    }
  }

  @Override
  public String toString() {
    return "FlushPageContextImpl [super=" + super.toString() + ", addressesToFlush="
        + pageAddressToFlush + "]";
  }
}
