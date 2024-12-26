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
