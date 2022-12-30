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
