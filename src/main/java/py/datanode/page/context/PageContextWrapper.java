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
