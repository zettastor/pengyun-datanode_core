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

import java.util.concurrent.CountDownLatch;
import py.archive.page.PageAddress;
import py.datanode.page.Page;
import py.datanode.page.TaskType;
import py.datanode.page.impl.BogusPageAddress;

public class CleanCachePageContextImpl<P extends Page> extends AbstractBasePageContext<P> {
  private final CountDownLatch latch;

  private final PageAddress pageAddress = new BogusPageAddress();

  public CleanCachePageContextImpl(TaskType taskType, CountDownLatch latch) {
    super(taskType);
    this.latch = latch;
  }

  @Override
  public PageAddress getPageAddressForIo() {
    return pageAddress;
  }

  @Override
  public void waitFor() throws InterruptedException {
    if (latch != null) {
      latch.await();
    }
  }

  @Override
  public void done() {
    if (latch != null) {
      latch.countDown();
    }
  }
}
