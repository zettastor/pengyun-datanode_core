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

public class AverageValueCollector {
  private final AtomicLong total = new AtomicLong(0);
  private final AtomicLong count = new AtomicLong(0);

  public void submit(long value) {
    total.addAndGet(value);
    count.incrementAndGet();
  }

  public long average() {
    long currentTotal = total.getAndSet(0);
    long currentCount = count.getAndSet(0);

    if (currentCount == 0) {
      return 0;
    } else {
      return currentTotal / currentCount;
    }
  }
}
