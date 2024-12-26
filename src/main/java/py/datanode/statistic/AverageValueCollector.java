

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
