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

package py.datanode.storage.cache;

import java.util.Random;
import py.common.SlidingTimeWindowMeter;
import py.test.TestBase;

public class IoRecorderTest extends TestBase {
  private Random random = new Random(System.currentTimeMillis());

  public void test() throws InterruptedException {
    SlidingTimeWindowMeter recorder = new SlidingTimeWindowMeter(5, 1000);

    new Thread(() -> {
      while (true) {
        recorder.mark();
        try {
          Thread.sleep(random.nextInt(10));
        } catch (InterruptedException ignore) {
          logger.error("InterruptedException", ignore);
        }
      }
    }).start();

    while (true) {
      logger.warn("val {}", recorder.getAll());
      Thread.sleep(1000);
    }
  }
}