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