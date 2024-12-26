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

package py.datanode.storage.scheduler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.datanode.storage.context.StorageIoContext;
import py.function.SimpleCallable;

abstract class StorageIoQueue<E extends StorageIoContext> {
  protected static final Logger logger = LoggerFactory.getLogger(StorageIoQueue.class);

  private final SimpleCallable availableTasksListener;

  private final int lowerPendingTaskThreshold;
  private final int upperPendingTaskThreshold;

  private final AtomicInteger leftCount = new AtomicInteger(0);

  private final AtomicInteger submittedCount = new AtomicInteger(0);

  StorageIoQueue(SimpleCallable availableTasksListener, int lowerPendingTaskThreshold,
      int upperPendingTaskThreshold) {
    this.availableTasksListener = availableTasksListener;
    this.lowerPendingTaskThreshold = lowerPendingTaskThreshold;
    this.upperPendingTaskThreshold = upperPendingTaskThreshold;
  }

  protected abstract void enqueue(E element);

  protected abstract void drainTasks(Collection<E> elementsContainer, int maxCount);

  protected abstract int queueLength();

  void offer(E element) {
    leftCount.getAndIncrement();
    enqueue(element);
    if (submittedCount.get() == 0) {
      availableTasksListener.call();
    }
  }

  void generateContexts(Collection<StorageIoContext> container, int maxCount) {
    if (submittedCount.get() > lowerPendingTaskThreshold) {
      return;
    }

    List<E> elementList = new ArrayList<>();
    drainTasks(elementList, Math.min(upperPendingTaskThreshold - submittedCount.get(), maxCount));

    for (E context : elementList) {
      context.addFinishHooker(this::decrement);
      container.add(context);
      leftCount.decrementAndGet();
      submittedCount.incrementAndGet();
    }
  }

  boolean hasAvailableTasks() {
    logger.debug("has available tasks ? submitted {}, left {}", submittedCount.get(),
        leftCount.get());
    return submittedCount.get() <= lowerPendingTaskThreshold && leftCount.get() > 0;
  }

  private void decrement() {
    if (submittedCount.decrementAndGet() <= lowerPendingTaskThreshold && leftCount.get() > 0) {
      availableTasksListener.call();
    }
  }

}
