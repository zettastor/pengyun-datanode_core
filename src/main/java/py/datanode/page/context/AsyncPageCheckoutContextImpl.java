

package py.datanode.page.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.archive.page.PageAddress;
import py.datanode.page.Page;
import py.datanode.page.PageContextCallback;
import py.datanode.page.TaskType;

public class AsyncPageCheckoutContextImpl<T extends Page> extends AbstractGetPageContext<T> {
  private static final Logger logger = LoggerFactory.getLogger(AsyncPageCheckoutContextImpl.class);
  private PageContextCallback<T> callback;

  public AsyncPageCheckoutContextImpl(PageAddress pageAddressToLoad, TaskType taskType,
      PageContextCallback<T> callback) {
    super(pageAddressToLoad, taskType);
    this.callback = callback;
  }

  @Override
  public void done() {
    logger.debug("done! {} going to callback completed {}", this, callback);
    callback.completed(this);
  }

  @Override
  public String toString() {
    return "AsyncPageCheckoutContextImpl [super=" + super.toString() + ", notifyAllListeners="
        + callback + "]";
  }

}
