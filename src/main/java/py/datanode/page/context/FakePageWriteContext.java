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

import py.archive.page.PageAddress;
import py.datanode.page.Page;
import py.engine.BogusLatency;
import py.engine.Latency;

public class FakePageWriteContext<T extends Page> extends ComparablePageContext<T> {
  private final PageAddress pageAddressForIo;
  private Latency latency = BogusLatency.DEFAULT;

  public FakePageWriteContext(PageAddress pageAddressForIo) {
    this.pageAddressForIo = pageAddressForIo;
  }

  @Override
  public PageAddress getPageAddressForIo() {
    return pageAddressForIo;
  }

  @Override
  public Latency getLatency() {
    return latency;
  }

  @Override
  public void setLatency(Latency latency) {
    this.latency = latency;
  }

  @Override
  public String toString() {
    return "FakePageWriteContext [super=" + super.toString() + ", pageAddressForIo="
        + pageAddressForIo + "]";
  }
}
