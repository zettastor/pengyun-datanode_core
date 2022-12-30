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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import py.datanode.storage.context.StorageIoContext;
import py.function.SimpleCallable;

public class SingleStorageIoQueue<E extends StorageIoContext> extends StorageIoQueue<E> {
  private final BlockingQueue<E> elementQueue;

  SingleStorageIoQueue(SimpleCallable availableTasksListener, int lowerPendingTaskThreshold,
      int upperPendingTaskThreshold) {
    super(availableTasksListener, lowerPendingTaskThreshold, upperPendingTaskThreshold);
    this.elementQueue = new LinkedBlockingQueue<>();
  }

  @Override
  protected void enqueue(E element) {
    elementQueue.offer(element);
  }

  @Override
  protected void drainTasks(Collection<E> elementsContainer, int maxCount) {
    elementQueue.drainTo(elementsContainer, maxCount);
  }

  @Override
  protected int queueLength() {
    return elementQueue.size();
  }

}
