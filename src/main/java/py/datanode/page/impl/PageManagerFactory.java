
package py.datanode.page.impl;

import py.datanode.page.Page;
import py.datanode.page.PageManager;

public interface PageManagerFactory {
  PageManager<Page> build(long size, String name);
}
