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
