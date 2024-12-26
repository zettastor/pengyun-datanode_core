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