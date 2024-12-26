

package py.datanode.page.context;

import py.datanode.page.Page;

public class SinglePageContextFactory {
  private static final PageContextFactory<Page> factory = new PageContextFactory<Page>();

  private SinglePageContextFactory() {
  }

  public static PageContextFactory<Page> getInstance() {
    return factory;
  }

}
