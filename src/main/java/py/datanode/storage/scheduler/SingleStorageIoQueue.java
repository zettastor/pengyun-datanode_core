

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
