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
