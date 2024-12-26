

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
