
package py.datanode.page;

import py.storage.Storage;

public interface AvailablePageLister {
  void hasAvailblePage();

  void hasAvailblePage(Storage storage);
}
