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

import java.util.Collection;
import py.datanode.page.Page;
import py.datanode.page.PageContext;
import py.engine.BogusLatency;
import py.engine.Latency;

public class PageContextWrapper<T extends Page> extends AbstractPageContext<T> {
  protected final Collection<PageContext<T>> pageContexts;
  private Latency latency = BogusLatency.DEFAULT;

  public PageContextWrapper(Collection<PageContext<T>> pageContexts) {
    this.pageContexts = pageContexts;
  }

  public Collection<PageContext<T>> getPageContexts() {
    return pageContexts;
  }

  @Override
  public String toString() {
    return "PageContextWrapper [pageContexts=" + pageContexts + "]";
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
