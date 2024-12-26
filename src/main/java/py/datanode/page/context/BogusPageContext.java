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

import py.archive.page.PageAddress;
import py.datanode.page.IoType;
import py.datanode.page.Page;
import py.datanode.page.PageContext;
import py.datanode.page.TaskType;
import py.datanode.page.impl.PageAddressGenerator;
import py.engine.BogusLatency;
import py.engine.Latency;
import py.storage.Storage;

public class BogusPageContext<T extends Page> extends ComparablePageContext<T> {
  public static final PageContext<Page> defaultBogusPageContext = new BogusPageContext<Page>();

  /**
   * the PageAddress is used for sorting the Page Context when doing I/O operators. It is sure that
   * the BogusPageContext is in the end.
   */
  private static final PageAddress pageAddress = PageAddressGenerator.generateMaxPageAddress();
  public PageContext<T> pageContext;
  private Latency latency = BogusLatency.DEFAULT;

  public BogusPageContext() {
    this(null);
  }

  public BogusPageContext(PageContext<T> context) {
    this.pageContext = context;
  }

  @Override
  public T getPage() {
    return null;
  }

  @Override
  public void setPage(T page) {
  }

  @Override
  public void waitFor() throws InterruptedException {
  }

  @Override
  public void done() {
  }

  @Override
  public PageAddress getPageAddressForIo() {
    return pageAddress;
  }

  @Override
  public TaskType getTaskType() {
    return null;
  }

  @Override
  public void setTaskType(TaskType taskType) {
  }

  @Override
  public boolean isSuccess() {
    return false;
  }

  @Override
  public Exception getCause() {
    return null;
  }

  @Override
  public void setCause(Exception e) {
  }

  @Override
  public IoType getIoType() {
    return null;
  }

  @Override
  public void setIoType(IoType ioType) {
  }

  @Override
  public void setExpiredTime(long expiredTime) {
  }

  @Override
  public boolean isExpired() {
    return false;
  }

  @Override
  public PageContext<T> getOriginalPageContext() {
    return null;
  }

  public PageAddress getPageAddressForCompare() {
    return pageAddress;
  }

  @Override
  public String toString() {
    return "BogusPageContext [pageContext=" + pageContext + "]";
  }

  @Override
  public Storage getStorage() {
    return null;
  }

  @Override
  public void cancel() {
  }

  @Override
  public boolean isCancel() {
    return false;
  }

  @Override
  public Latency getLatency() {
    return latency;
  }

  @Override
  public void setLatency(Latency latency) {
    this.latency = latency;
  }
}
