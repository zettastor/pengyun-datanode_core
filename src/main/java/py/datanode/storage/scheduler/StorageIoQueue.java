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
