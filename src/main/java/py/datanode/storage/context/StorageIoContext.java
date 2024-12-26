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

package py.datanode.storage.context;

import java.util.ArrayList;
import java.util.List;
import py.function.Callback;
import py.function.SimpleCallable;
import py.io.sequential.IoSequentialTypeHolder;

/**
 * Storage IO Context, holding requests to storage.
 */
public abstract class StorageIoContext implements Callback, IoSequentialTypeHolder {
  protected final Callback callback;

  private final StorageIoType ioType;
  private final List<SimpleCallable> hookerList;
  private IoSequentialType ioSequentialType;
  private long offset;
  private int ioLength;

  protected StorageIoContext(StorageIoType ioType, Callback callback) {
    this(ioType, callback, 0, 0);
  }

  protected StorageIoContext(StorageIoType ioType, Callback callback, long offset, int length) {
    this.ioType = ioType;
    this.callback = callback;
    this.hookerList = new ArrayList<>();
    this.ioSequentialType = IoSequentialType.UNKNOWN;
    this.offset = offset;
    this.ioLength = length;
  }

  @Override
  public void completed() {
    try {
      callback.completed();
    } finally {
      invokeFinishHooks();
    }
  }

  @Override
  public void failed(Throwable exc) {
    try {
      callback.failed(exc);
    } finally {
      invokeFinishHooks();
    }
  }

  private void invokeFinishHooks() {
    synchronized (hookerList) {
      hookerList.forEach(SimpleCallable::call);
    }
  }

  public void addFinishHooker(SimpleCallable hooker) {
    synchronized (hookerList) {
      hookerList.add(hooker);
    }
  }

  public void discard() {
    invokeFinishHooks();
  }

  @Override
  public long getOffset() {
    return offset;
  }

  @Override
  public int getLength() {
    return ioLength;
  }

  @Override
  public IoSequentialType getIoSequentialType() {
    return ioSequentialType;
  }

  @Override
  public void setIoSequentialType(IoSequentialType ioSequentialType) {
    this.ioSequentialType = ioSequentialType;
  }

  public StorageIoType getIoType() {
    return ioType;
  }

  public abstract boolean markProcessing();

  public abstract long getOffsetOnArchive();

  public abstract long getStartTime();

  @Override
  public String toString() {
    return "StorageIOContext{" + "ioType=" + ioType + ", callback=" + callback + ", hookerList="
        + hookerList + '}';
  }

  public enum StorageIoType {
    /**
     * Read data.
     */
    READ(1),
    /**
     * Write data.
     */
    WRITE(2);

    private int val;

    StorageIoType(int val) {
      this.val = val;
    }

    public int val() {
      return val;
    }
  }
}
