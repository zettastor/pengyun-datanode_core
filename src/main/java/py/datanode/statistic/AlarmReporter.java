
package py.datanode.statistic;

import java.util.function.Supplier;

public interface AlarmReporter {
  void register(Supplier<AlarmReportData> alarmSupplier, long reportRateInMs);

  void submit(AlarmReportData data);
}
