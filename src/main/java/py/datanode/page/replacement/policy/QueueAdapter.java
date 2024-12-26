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

package py.datanode.page.replacement.policy;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;

public class QueueAdapter<E> implements Queue<E> {
  private final Collection<E> fifoCollection;

  public QueueAdapter(Collection<E> fifoCollection) {
    if (fifoCollection == null) {
      throw new NullPointerException();
    }
    this.fifoCollection = fifoCollection;
  }

  public E element() {
    if (fifoCollection.isEmpty()) {
      throw new NoSuchElementException();
    }
    return fifoCollection.iterator().next();
  }

  public boolean offer(E e) {
    try {
      return fifoCollection.add(e);
    } catch (UnsupportedOperationException uoe) {
      return false;
    } catch (IllegalStateException ise) {
      return false;
    }
  }

  public E peek() {
    if (fifoCollection.isEmpty()) {
      return null;
    }
    return fifoCollection.iterator().next();
  }

  public E poll() {
    if (fifoCollection.isEmpty()) {
      return null;
    }
    E value = fifoCollection.iterator().next();
    fifoCollection.remove(value);
    return value;
  }

  public boolean remove(Object o) {
    return fifoCollection.remove(o);
  }
  
  
  public E remove() {
    E value = poll();
    if (value == null) {
      throw new NoSuchElementException();
    }
    return value;
  }

  public boolean add(E e) {
    return fifoCollection.add(e);
  }

  public boolean addAll(Collection<? extends E> c) {
    return fifoCollection.addAll(c);
  }

  public void clear() {
    fifoCollection.clear();
  }

  public boolean contains(Object o) {
    return fifoCollection.contains(o);
  }

  public boolean containsAll(Collection<?> c) {
    return fifoCollection.containsAll(c);
  }

  public boolean isEmpty() {
    return fifoCollection.isEmpty();
  }

  public Iterator<E> iterator() {
    return fifoCollection.iterator();
  }

  public boolean removeAll(Collection<?> c) {
    return fifoCollection.removeAll(c);
  }

  public boolean retainAll(Collection<?> c) {
    return fifoCollection.retainAll(c);
  }

  public int size() {
    return fifoCollection.size();
  }

  public Object[] toArray() {
    return fifoCollection.toArray();
  }

  public <T> T[] toArray(T[] a) {
    return fifoCollection.toArray(a);
  }

  @Override
  public String toString() {
    return fifoCollection.toString();
  }
}
