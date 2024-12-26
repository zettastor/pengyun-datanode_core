

package py.datanode.page.context;

import py.datanode.page.Page;
import py.engine.BogusLatency;
import py.engine.Latency;
import py.storage.Storage;

public class StorageDirtyPageContextImpl<P extends Page> extends AbstractPageContext<P> {
  private final Storage storage;
  private Latency latency = BogusLatency.DEFAULT;

  public StorageDirtyPageContextImpl(Storage storage) {
    this.storage = storage;
  }

  public Storage getStorage() {
    return storage;
  }

  @Override
  public Latency getLatency() {
    return latency;
  }

  @Override
  public void setLatency(Latency latency) {
    this.latency = latency;
  }
}
