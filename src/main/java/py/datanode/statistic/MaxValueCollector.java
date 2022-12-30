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

package py.datanode.statistic;

import java.util.concurrent.atomic.AtomicLong;

public class MaxValueCollector {
  private final AtomicLong value;
  private final AtomicLong maxValue;
  private final long initValue;

  public MaxValueCollector(long initValue) {
    this.initValue = initValue;
    this.value = new AtomicLong(initValue);
    this.maxValue = new AtomicLong(initValue);
  }

  public void inc(long n) {
    long newValue = value.addAndGet(n);

    if (newValue > maxValue.get()) {
      maxValue.set(newValue);
    }
  }

  public long getAndResetMaxValue() {
    return maxValue.getAndSet(initValue);
  }
}
