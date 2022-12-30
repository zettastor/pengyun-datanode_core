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
