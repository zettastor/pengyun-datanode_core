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

package py.datanode.page.replacement.policy;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReplacementPageManagerImpl<T> implements ReplacementPageManager<T> {
  private static final Logger logger = LoggerFactory.getLogger(ReplacementPageManagerImpl.class);
  /**
   * The max number of pages in page system.
   */
  private final int maxElements;
  /**
   * It is twice of the max number of elements in cache.
   */
  private final int maxWorkingSetSize;
  /**
   * The queue keeps the elements which are hit only once.
   */
  private Queue<T> hitOnceQueue;
  /**
   * The queue keeps the elements which are hit at least twice.
   */
  private Queue<T> hitMoreQueue;
  /**
   * The queue is used to balance the queue of hitOnceQueue.
   */
  private Queue<T> hitOnceBalanceQueue;
  /**
   * The queue is used to balance the queue of hitMoreQueue.
   */
  private Queue<T> hitMoreBalanceQueue;
  /**
   * The map keep the relationship between Queue and element, you can find the element belong to
   * which queue though performance.
   */
  private Map<T, Queue<T>> mapElementToQueue;
  /**
   * When a element is remove from {@link #hitOnceQueue} or {@link #hitMoreQueue}, it should notify
   * the listener.
   */
  private ReplacementPageManagerListener<T> listener;
  /**
   * There is a threshold for ensuring that the {@link #hitOnceQueue} and {@link #hitMoreQueue} are
   * in balance status.
   */
  private int threshold;
  private Map<Queue<T>, String> queueNames = new IdentityHashMap<Queue<T>, String>();

  public ReplacementPageManagerImpl(int maxElements, ReplacementPageManagerListener<T> listener) {
    this.hitOnceQueue = new QueueAdapter<T>(new LinkedHashSet<T>());
    this.hitMoreQueue = new QueueAdapter<T>(new LinkedHashSet<T>());
    this.hitOnceBalanceQueue = new QueueAdapter<T>(new LinkedHashSet<T>());
    this.hitMoreBalanceQueue = new QueueAdapter<T>(new LinkedHashSet<T>());
    this.maxElements = maxElements;
    this.mapElementToQueue = new HashMap<T, Queue<T>>();
    this.maxWorkingSetSize = maxElements * 2;
    this.listener = listener;

    this.queueNames.put(hitOnceQueue, "hitOnceQueue");
    this.queueNames.put(hitMoreQueue, "hitMoreQueue");
    this.queueNames.put(hitOnceBalanceQueue, "hitOnceBalanceQueue");
    this.queueNames.put(hitMoreBalanceQueue, "hitMoreBalanceQueue");
  }

  public void setListener(ReplacementPageManagerListener<T> listener) {
    this.listener = listener;
  }

  @Override
  public void clear() {
    hitOnceQueue.clear();
    hitMoreQueue.clear();
    hitOnceBalanceQueue.clear();
    hitMoreBalanceQueue.clear();
    mapElementToQueue.clear();
  }
  

  private T remove(Queue<T> from, boolean checkcanEvict) {
    if (checkcanEvict) {
      T element = from.peek();
      if (element == null || !listener.canEvict(element)) {
        return null;
      }
    }

    T element = from.remove();
    logger.info("remove the head:{} element from queue: {}", element, name(from));
    if (element != null) {
      mapElementToQueue.remove(element);
    }

    return element;
  }

  @Override
  public boolean remove(T element) {
    logger.info("remove the element: {}", element);
    Queue<T> hitQueue = mapElementToQueue.get(element);
    if (hitQueue != null) {
      if (hitQueue == hitOnceQueue || hitQueue == hitMoreQueue) {
        if (!listener.canEvict(element)) {
          logger.info("can not evict the element={}", element);
          return false;
        }
      }

      boolean success = hitQueue.remove(element);
      mapElementToQueue.remove(element);
      return success;
    } else {
      logger.info("element={} is not exist", element);
      return false;
    }
  }

  @Override
  public void visit(T element) {
    Queue<T> currentQueue = mapElementToQueue.get(element);

    if (currentQueue == null) {
      onMiss(element);
      return;
    }

    if (currentQueue == hitOnceQueue || currentQueue == hitMoreQueue) {
      moveElement(element, currentQueue, hitMoreQueue);
      return;
    }

    T replaceElement = null;

    if (currentQueue == hitOnceBalanceQueue) {
      int tmpThreshold = Math.min(
          threshold + Math.max(hitMoreBalanceQueue.size() / hitOnceBalanceQueue.size(), 1),
          maxElements);
      if (balance()) {
        threshold = tmpThreshold;
      }

    } else {
      int tmpThreshold = Math
          .max(threshold - Math.max(hitOnceBalanceQueue.size() / hitMoreBalanceQueue.size(), 1), 0);
      if (hitOnceQueue.size() == threshold && !hitOnceQueue.isEmpty()) {
        replaceElement = moveToBalanceQueue(hitOnceQueue, hitOnceBalanceQueue);
        if (replaceElement != null) {
          listener.evict(replaceElement);
          threshold = tmpThreshold;
        }
      } else {
        if (balance()) {
          threshold = tmpThreshold;
        }
      }
    }

    moveElement(element, currentQueue, hitMoreQueue);
  }

  @Override
  public void free() {
    onMiss(null);
  }

  public void onMiss(T element) {
    T replaceElement = null;
    if (hitOnceQueue.size() + hitOnceBalanceQueue.size() >= maxElements) {
      if (hitOnceQueue.size() < maxElements) {
        logger.debug(
            "remove the head element of hit-once-balance-queue and " 
                + "move the head element of hit-once-queue to the tail of hit-once-balance-queue");
        remove(hitOnceBalanceQueue, false);
        balance();
      } else {
        replaceElement = remove(hitOnceQueue, true);
        logger.debug("remove the head element:{} from hit-once-queue", replaceElement);
        if (replaceElement != null) {
          listener.evict(replaceElement);
        }
      }
    } else {
      int currentWorkingSetSize = currentWorkingSetSize();
      if (currentWorkingSetSize >= maxElements) {
        if (currentWorkingSetSize >= maxWorkingSetSize) {
          logger.debug("remove the head of hit-more-balance-queue");
          remove(hitMoreBalanceQueue, false);
        }

        logger
            .debug("move the head element of hit-more-queue to the tail of hit-more-balance-queue");
        balance();
      } else {
        logger.debug("There is enough space, so there is no need to remove the element");
      }
    }

    if (element != null) {
      logger.info("insert the element {} to tail queue:{}", element, name(hitOnceQueue));
      hitOnceQueue.add(element);
      mapElementToQueue.put(element, hitOnceQueue);
    }
  }

  public int currentWorkingSetSize() {
    return hitOnceQueue.size() + hitOnceBalanceQueue.size() + hitMoreQueue.size()
        + hitMoreBalanceQueue.size();
  }

  private void moveElement(T element, Queue<T> from, Queue<T> to) {
    logger.debug("move the element {} from {} to {}", element, name(from), name(to));
    from.remove(element);
    to.add(element);
    mapElementToQueue.put(element, to);
  }

  private T moveToBalanceQueue(Queue<T> from, Queue<T> to) {
    T element = from.peek();
    if (element == null || !listener.canEvict(element)) {
      return null;
    }

    Validate.isTrue(element == from.remove());
    to.add(element);
    mapElementToQueue.put(element, to);
    logger.debug("move {} the head of queue: {} to the tail of queue {}", element, name(from),
        name(to));
    return element;
  }

  private boolean balance() {
    T element = null;
    if (!hitOnceQueue.isEmpty() && hitOnceQueue.size() > threshold) {
      logger.debug("do balance between hitOnceQueue and hitOnceBalanceQueue");
      element = moveToBalanceQueue(hitOnceQueue, hitOnceBalanceQueue);
      if (element != null) {
        listener.evict(element);
      }
    } else {
      if (hitMoreQueue.isEmpty()) {
        logger.debug("hitMoreQueue is empty");
        return true;
      }

      logger.debug("do balance between hitMoreQueue and hitMoreBalanceQueue");
      element = moveToBalanceQueue(hitMoreQueue, hitMoreBalanceQueue);
      if (element != null) {
        listener.evict(element);
      }
    }

    return element == null ? false : true;
  }

  public String name(Queue<T> queue) {
    return queueNames.get(queue);
  }

  @Override
  public int size() {
    return hitOnceQueue.size() + hitMoreQueue.size();
  }

  public Set<T> getAllElements() {
    return mapElementToQueue.keySet();
  }

  public void dump() {
    for (Map.Entry<Queue<T>, String> entry : queueNames.entrySet()) {
      logger.info("queue: {} = size {}", entry.getValue(), entry.getKey().size());
    }
    logger.info("total element: {}", mapElementToQueue.size());
  }
}
