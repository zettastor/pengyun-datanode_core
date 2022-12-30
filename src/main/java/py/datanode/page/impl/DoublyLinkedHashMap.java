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

package py.datanode.page.impl;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.Validate;

/**
 * A "map" that acts as a cache.
 */
public class DoublyLinkedHashMap<K, V> {
  private int MAX_FREE_ENTRIES = 100;
  private final Map<K, DoublyLinkedHashMap.Entry<K, V>> map = new HashMap<K, DoublyLinkedHashMap.Entry<K, V>>();
  private DoublyLinkedHashMap.Entry<K, V> header = null;
  private DoublyLinkedHashMap.Entry<K, V> freeEntriesHeader = null;
  private int freeEntriesCount = 0;

  public DoublyLinkedHashMap() {
    initLists();
  }

  private void initLists() {
    header = new DoublyLinkedHashMap.Entry<>();
    header.after = header.before = header;
    freeEntriesHeader = new DoublyLinkedHashMap.Entry<>();
    freeEntriesHeader.after = freeEntriesHeader.before = freeEntriesHeader;
  }

  public V get(K key) {
    DoublyLinkedHashMap.Entry<K, V> entry = map.get(key);
    return entry == null ? null : entry.getValue();
  }

  public int size() {
    return map.size();
  }

  private boolean isEmpty() {
    boolean empty  = map.size() == 0;
    if (empty) {
      Validate.isTrue(header.after == header.before);
    }
    return empty;
  }

  public V putFirst(K key, V value) {
    Validate.notNull(key);
    V existingValue = null;
    DoublyLinkedHashMap.Entry<K, V> entry = map.get(key);
    if (entry == null) {
      entry = createEntry(key, value);
      map.put(key, entry);
    } else {
      entry.setValue(value);
      entry.removeMyself();
      existingValue = entry.value;
    }
    entry.addBefore(header);
    return existingValue;
  }

  public V removeLastValue() {
    if (isEmpty()){
      return null;
    }

    DoublyLinkedHashMap.Entry<K,V> tail = header.after;
    Validate.isTrue(tail != header);
    return remove(tail.getKey());
  }

  public V putLast(K key, V value) {
    Validate.notNull(key);
    V existingValue = null;
    DoublyLinkedHashMap.Entry<K, V> entry = map.get(key);
    if (entry == null) {
      entry = createEntry(key, value);
      map.put(key, entry);
    } else {
      entry.setValue(value);
      entry.removeMyself();
      existingValue = entry.value;
    }
    entry.addAfter(header);
    return existingValue;
  }

  private DoublyLinkedHashMap.Entry<K, V> createEntry(K k, V v) {
    DoublyLinkedHashMap.Entry<K, V> newEntry;
    if (freeEntriesHeader.isAlone()) {
      // not free entries
      newEntry = new DoublyLinkedHashMap.Entry<K, V>();
    } else {
      Validate.isTrue(freeEntriesCount > 0);
      freeEntriesCount--;
      newEntry = freeEntriesHeader.removeBefore();
    }
    newEntry.setKey(k);
    newEntry.setValue(v);
    return newEntry;
  }

  // the entry is supposed to be removed from the list already
  private void releaseEntry(DoublyLinkedHashMap.Entry<K, V> entry) {
    // Neutralize the entry, so that it can be GCed later
      entry.key = null;
      entry.value = null;
      entry.before = null;
      entry.after = null;

    if (freeEntriesCount < MAX_FREE_ENTRIES) {
      entry.addBefore(freeEntriesHeader);
    freeEntriesCount++;
    } //else too many free entry, just GC it.
  }

  public V remove(K key) {
    if (isEmpty()){
      return null;
    }

    DoublyLinkedHashMap.Entry<K, V> entry = map.remove(key);
    if (entry == null) {
      return null;
    } else {
      entry.removeMyself();
      V value = entry.getValue();
      releaseEntry(entry);
      return value;
    }
  }

  private static class Entry<K,V> implements Map.Entry<K,V> {
    // These fields comprise the doubly linked list used for iteration.
    DoublyLinkedHashMap.Entry<K,V> before;
    DoublyLinkedHashMap.Entry<K,V> after;
    private K key;
    private V value;

    public Entry() {
      before = after = this;
    }

    @Override
    public K getKey() {
      return key;
    }

    void setKey(K key) {
      this.key = key;
    }

    @Override
    public V getValue() {
      return value;
    }

    @Override
    public V setValue(V newValue) {
      V existing = value;
      value = newValue;
      return existing;
    }

    /**
     * Removes this entry from the linked list.
     */
    private void removeMyself() {
      before.after = after;
      after.before = before;
    }

    /**
     * Removes the entry before mine
     */
    private DoublyLinkedHashMap.Entry<K, V> removeBefore() {
      Validate.notNull(before);
      if (isAlone()) {
        return null;
      }

      DoublyLinkedHashMap.Entry<K,V> beforeEntry = before;
      beforeEntry.removeMyself();
      return beforeEntry;
    }

      /**
       * Inserts this entry before the specified existing entry in the list.
       */
    private void addBefore(DoublyLinkedHashMap.Entry<K,V> existingEntry) {
      after  = existingEntry;
      before = existingEntry.before;
      before.after = this;
      after.before = this;
    }

    /**
     * Inserts this entry after the specified existing entry in the list.
     */
    private void addAfter(DoublyLinkedHashMap.Entry<K,V> existingEntry) {
      after = existingEntry.after;
      existingEntry.after = this;
      before = existingEntry;
      after.before = this;
    }

    boolean isAlone() {
      if ((after == null && before == null) || (after == this && before == this)) {
        return true;
      } else {
        return false;
      }
    }
  }

  // for unit tests
  protected int getFreeEntryCount() {
    return freeEntriesCount;
  }

  protected void setMaxFreeEntryCount(int max) {
    MAX_FREE_ENTRIES = max;
  }

}
