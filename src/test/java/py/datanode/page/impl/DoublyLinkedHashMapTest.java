package py.datanode.page.impl;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.junit.Assert;
import org.junit.Test;
import py.test.TestBase;

public class DoublyLinkedHashMapTest extends TestBase {

  private List<Integer> sources = new ArrayList<>();
  private Random random;
  private DoublyLinkedHashMap<Integer, Integer> cache;
  private int count = 200;

  @Test
  public void testPutFirst() throws Exception {
    prepareSource();

    // test putFirst
    for (Integer s : sources) {
      logger.debug("s:{}", s);
      Assert.assertNull(cache.putFirst(s, s));
    }

    Assert.assertEquals(count, cache.size());

    int index = 0;
    for (Integer s : sources) {
      Integer testS = cache.removeLastValue();
      Assert.assertEquals(s, testS);
    }
    Assert.assertEquals(0, cache.size());
  }

  @Test
  public void testPutFirstExistingKey() throws Exception {
    prepareSource();

    Integer middleElement = null;
    // test putFirst
    int index = 0;
    for (Integer s : sources) {
      logger.debug("s:{}", s);
      if (index++ == count / 2) {
        middleElement = s;
      }
      cache.putFirst(s, s);
    }

    Assert.assertEquals(count, cache.size());

    cache.putFirst(middleElement, middleElement);
    Assert.assertEquals(count, cache.size());

    // remove all entries and the last one is the middleElement
    index = 0;
    while (index < count - 1) {
      cache.removeLastValue();
      index++;
    }

    Assert.assertEquals(1, cache.size());
    Integer expected = cache.removeLastValue();
    Assert.assertEquals(expected, middleElement);
  }

  @Test
  public void testPutLast() throws Exception {
    prepareSource();
    // test putLast
    for (Integer s : sources) {
      logger.debug("s:{}", s);
      Assert.assertNull(cache.putLast(s, s));
    }

    Assert.assertEquals(count, cache.size());

    Collections.reverse(sources);
    for (Integer s : sources) {
      Integer testS = cache.removeLastValue();
      Assert.assertEquals(s, testS);
    }
    Assert.assertEquals(0, cache.size());
    // add a new node to the cache
    Integer newInt = random.nextInt();

    cache.putLast(newInt, newInt);
    Assert.assertEquals(newInt, cache.removeLastValue());
    Assert.assertEquals(0, cache.size());
  }

  @Test
  public void testPutLastExistingKey() throws Exception {
    prepareSource();

    Integer middleElement = null;
    // test putFirst
    int index = 0;
    for (Integer s : sources) {
      logger.debug("s:{}", s);
      if (index++ == count / 2) {
        middleElement = s;
      }
      cache.putFirst(s, s);
    }

    Assert.assertEquals(count, cache.size());

    cache.putLast(middleElement, middleElement);
    Assert.assertEquals(count, cache.size());

    Integer expected = cache.removeLastValue();
    Assert.assertEquals(expected, middleElement);
  }


  @Test
  public void testFreeEntries() throws Exception {
    prepareSource();
    cache.setMaxFreeEntryCount(count);

    for (Integer s : sources) {
      logger.debug("s:{}", s);
      cache.putFirst(s, s);
      // not from the free entries
      int freeEntriesCountFromCache = cache.getFreeEntryCount();
      Assert.assertEquals(0, freeEntriesCountFromCache);
    }

    int freeCount = 0;
    for (Integer s : sources) {
      Integer testS = cache.removeLastValue();
      freeCount++;
      int freeEntriesCountFromCache = cache.getFreeEntryCount();
      Assert.assertEquals(freeCount, freeEntriesCountFromCache);
    }

    freeCount = count;
    for (Integer s : sources) {
      logger.debug("s:{}", s);
      cache.putFirst(s, s);
      freeCount--;
      // not from the free entries
      int freeEntriesCountFromCache = cache.getFreeEntryCount();
      Assert.assertEquals(freeCount, freeEntriesCountFromCache);
    }
  }


  @Test
  public void testRemoveExistingKey() throws Exception {
    prepareSource();

    Integer middleElement = null;
    // test putFirst
    int index = 0;
    for (Integer s : sources) {
      logger.debug("s:{}", s);
      if (index++ == count / 2) {
        middleElement = s;
      }
      cache.putFirst(s, s);
    }

    Integer value = cache.remove(random.nextInt());
    Assert.assertEquals(count, cache.size());
    Assert.assertNull(value);

    value = cache.remove(middleElement);
    Assert.assertEquals(count - 1, cache.size());
    Assert.assertEquals(middleElement, value);
  }

  private void prepareSource() {
    random = new Random(System.currentTimeMillis());
    cache = new DoublyLinkedHashMap<>();
    sources.clear();
    int index = 0;
    while (index < count) {
      Integer s = random.nextInt();
      //  logger.debug("index: {}, s:{}", index, s);
      sources.add(s);
      index++;
    }
  }
}