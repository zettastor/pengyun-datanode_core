
package py.datanode.page.impl;

import java.util.List;
import py.archive.page.PageAddress;
import py.datanode.page.PageManager;

public interface PageManagerDispatcher {
  <P extends PageManager> P select(List<P> candidates, PageAddress address);

}
