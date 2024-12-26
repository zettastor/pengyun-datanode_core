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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import py.datanode.page.Page;
import py.datanode.page.PageContext;
import py.datanode.page.PageContextCallback;

public class AsyncPageContextWaiter<T extends Page> implements PageContextCallback<T> {
  private LinkedBlockingQueue<PageContext<T>> queue;
  private int expectedCount;
  private int takeCount;

  public AsyncPageContextWaiter() {
    this.queue = new LinkedBlockingQueue<PageContext<T>>();
    this.expectedCount = 0;
    this.takeCount = 0;
  }

  /**
   * this method is called in one thread, so there is no need considering problems about multiple
   * thread.
   */
  public PageContext<T> take() throws InterruptedException {
    if (takeCount == expectedCount) {
      return null;
    }

    takeCount++;
    return queue.take();
  }

  public PageContext<T> take(long timeout, TimeUnit unit) throws InterruptedException {
    if (takeCount == expectedCount) {
      return null;
    }

    PageContext<T> context = queue.poll(timeout, unit);
    if (context != null) {
      takeCount++;
    }

    return context;
  }

  public PageContext<T> take1(long timeout, TimeUnit unit) throws InterruptedException {
    PageContext<T> task = queue.poll(timeout, unit);
    if (task != null) {
      takeCount++;
    }
    return task;
  }

  public List<PageContext<T>> drainTo() throws InterruptedException {
    if (takeCount == expectedCount) {
      return null;
    }

    List<PageContext<T>> result = new ArrayList<PageContext<T>>();
    takeCount += queue.drainTo(result);
    return result;
  }

  /**
   * this method is called in one thread, so there is no need considering problems about multiple
   * thread.
   */
  public void increment() {
    expectedCount++;
  }

  public void add(int count) {
    expectedCount += count;
  }

  /**
   * this method is called in one thread, so there is no need considering problems about multiple
   * thread.
   */
  public void decrement() {
    expectedCount--;
  }

  public int getPendingTaskCount() {
    return expectedCount - takeCount;
  }

  @Override
  public String toString() {
    return "AsyncPageContextWaiter [queue=" + (queue == null ? null : queue.size())
        + ", expectedCount="
        + expectedCount + ", takeCount=" + takeCount + "]";
  }

  @Override
  public void completed(PageContext<T> pageContext) {
    boolean success = queue.offer(pageContext);
    if (!success) {
      throw new IllegalArgumentException("can not add context: " + pageContext);
    }
  }
}
