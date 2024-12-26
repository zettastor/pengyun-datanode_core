
package py.datanode.page.impl;

import py.archive.page.PageAddress;
import py.datanode.page.Page;
import py.datanode.page.PageSelectionAlgorithm;

public class PageSelectionLruAlgorithm<P extends Page> implements PageSelectionAlgorithm<P> {
  public P select(DoublyLinkedHashMap<PageAddress, P> freePagePool) {
    return freePagePool.removeLastValue();
  }
}
