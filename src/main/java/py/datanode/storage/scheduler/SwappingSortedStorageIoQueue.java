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

import java.util.Collection;
import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;
import py.datanode.storage.context.StorageIoContext;
import py.function.SimpleCallable;

public class SwappingSortedStorageIoQueue<E extends StorageIoContext> extends StorageIoQueue<E> {
  private volatile PriorityBlockingQueue<E> offeringQueue;
  private volatile PriorityBlockingQueue<E> takingQueue;

  SwappingSortedStorageIoQueue(SimpleCallable availableTasksListener, int lowerPendingTaskThreshold,
      int upperPendingTaskThreshold, Comparator<E> comparator) {
    super(availableTasksListener, lowerPendingTaskThreshold, upperPendingTaskThreshold);
    if (comparator != null) {
      offeringQueue = new PriorityBlockingQueue<>(11, comparator);
      takingQueue = new PriorityBlockingQueue<>(11, comparator);
    } else {
      offeringQueue = new PriorityBlockingQueue<>(11);
      takingQueue = new PriorityBlockingQueue<>(11);
    }
  }

  private void swap() {
    PriorityBlockingQueue<E> tmp = offeringQueue;
    offeringQueue = takingQueue;
    takingQueue = tmp;
  }

  @Override
  protected void enqueue(E element) {
    offeringQueue.offer(element);
  }

  @Override
  protected void drainTasks(Collection<E> elementsContainer, int maxCount) {
    E element;
    boolean swapped = false;
    for (int i = 0; i < maxCount; i++) {
      element = takingQueue.poll();
      if (element != null) {
        elementsContainer.add(element);
      } else if (!swapped) {
        swap();
        swapped = true;
      } else {
        break;
      }
    }
  }

  @Override
  protected int queueLength() {
    return takingQueue.size() + offeringQueue.size();
  }
}
