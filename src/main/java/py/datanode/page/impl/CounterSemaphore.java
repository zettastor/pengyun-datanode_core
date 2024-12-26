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

package py.datanode.page.impl;

import java.util.concurrent.atomic.AtomicInteger;

public class CounterSemaphore {
  private final Object lock = new Object();
  public AtomicInteger counter;

  public CounterSemaphore() {
    counter = new AtomicInteger();
  }

  public int release() {
    int value = counter.incrementAndGet();
    if (value > 0) {
      throw new RuntimeException("release failure, value: " + value);
    }
    return value;
  }
  

  public int release(int count) {
    int value = counter.addAndGet(count);
    if (value > 0) {
      throw new RuntimeException("release too much, count: " + count + ", value: " + value);
    }
    return value;
  }

  public int get() {
    return counter.get();
  }

  public void await() throws InterruptedException {
    int value = counter.get();
    if (value == 0) {
      synchronized (lock) {
        value = counter.get();
        if (value == 0) {
          lock.wait();
        } else if (value > 0) {
          throw new RuntimeException("incredible, current value greater than " + value);
        }
      }
    } else if (value > 0) {
      throw new RuntimeException("current value greater than " + value);
    }
  }

  public void await(int timeoutMs) throws InterruptedException {
    int value = counter.get();
    if (value == 0) {
      synchronized (lock) {
        value = counter.get();
        if (value == 0) {
          lock.wait(timeoutMs);
        } else if (value > 0) {
          throw new RuntimeException("incredible, current value greater than " + value);
        }
      }
    } else if (value > 0) {
      throw new RuntimeException("current value greater than " + value);
    }
  }

  public int add() {
    int value = counter.getAndDecrement();
    if (value == 0) {
      synchronized (lock) {
        lock.notify();
      }
    }
    return --value;
  }
}
