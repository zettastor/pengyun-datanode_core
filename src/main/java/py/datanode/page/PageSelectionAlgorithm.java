
package py.datanode.page;

import py.archive.page.PageAddress;
import py.datanode.page.impl.DoublyLinkedHashMap;

/**
 * The algorithm to select a page and remove it from the pool.
 */
public interface PageSelectionAlgorithm<P extends Page> {
  public P select(final DoublyLinkedHashMap<PageAddress, P> freePagePool);
}
