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

import org.apache.commons.lang3.NotImplementedException;
import py.archive.page.PageAddress;
import py.datanode.page.IoType;
import py.datanode.page.Page;
import py.datanode.page.PageContext;
import py.datanode.page.TaskType;
import py.storage.Storage;

public abstract class AbstractPageContext<T extends Page> implements PageContext<T> {
  @Override
  public TaskType getTaskType() {
    throw new NotImplementedException("this is a BasePageContextImpl=" + toString());
  }

  @Override
  public void setTaskType(TaskType taskType) {
    throw new NotImplementedException("this is a BasePageContextImpl=" + toString());
  }

  @Override
  public T getPage() {
    throw new NotImplementedException("this is a BasePageContextImpl=" + toString());
  }

  @Override
  public void setPage(T page) {
    throw new NotImplementedException("this is a BasePageContextImpl=" + toString());
  }

  @Override
  public void waitFor() throws InterruptedException {
    throw new NotImplementedException("this is a BasePageContextImpl=" + toString());
  }

  @Override
  public void done() {
    throw new NotImplementedException("this is a BasePageContextImpl=" + toString());
  }

  @Override
  public PageAddress getPageAddressForIo() {
    throw new NotImplementedException("this is a BasePageContextImpl=" + toString());
  }

  public void setPageAddressForIo(PageAddress pageAdddressForIo) {
    throw new NotImplementedException("this is a BasePageContextImpl=" + toString());
  }

  @Override
  public boolean isSuccess() {
    throw new NotImplementedException("this is a BasePageContextImpl=" + toString());
  }

  @Override
  public Exception getCause() {
    throw new NotImplementedException("this is a BasePageContextImpl=" + toString());
  }

  @Override
  public void setCause(Exception e) {
    throw new NotImplementedException("this is a BasePageContextImpl=" + toString());
  }

  @Override
  public IoType getIoType() {
    throw new NotImplementedException("this is a BasePageContextImpl=" + toString());
  }

  @Override
  public void setIoType(IoType ioType) {
    throw new NotImplementedException("this is a BasePageContextImpl=" + toString());
  }

  @Override
  public void setExpiredTime(long expiredTime) {
    throw new NotImplementedException("this is a BasePageContextImpl=" + toString());
  }

  @Override
  public boolean isExpired() {
    throw new NotImplementedException("this is a BasePageContextImpl=" + toString());
  }

  @Override
  public PageContext<T> getOriginalPageContext() {
    throw new NotImplementedException("this is a BasePageContextImpl=" + toString());
  }

  @Override
  public Storage getStorage() {
    throw new NotImplementedException("this is a BasePageContextImpl=" + toString());
  }

  public void cancel() {
    throw new NotImplementedException("this is a BasePageContextImpl=" + toString());
  }

  public boolean isCancel() {
    throw new NotImplementedException("this is a BasePageContextImpl=" + toString());
  }
}