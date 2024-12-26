

package py.datanode.page.context;

import java.util.Comparator;
import py.archive.page.PageAddress;
import py.datanode.page.Page;
import py.datanode.page.PageContext;

public class PageContextComparator implements Comparator<PageContext<Page>> {
  @Override
  public int compare(PageContext<Page> o1, PageContext<Page> o2) {
    if (o1 == null) {
      if (o2 == null) {
        return 0;
      } else {
        return -1;
      }
    } else {
      if (o2 == null) {
        return 1;
      }
    }

    PageAddress addr1 = o1.getPageAddressForIo();
    PageAddress addr2 = o2.getPageAddressForIo();
    long diff = addr1.getPhysicalOffsetInArchive() - addr2.getPhysicalOffsetInArchive();
    if (diff > 0) {
      return 1;
    } else if (diff == 0) {
      return 0;
    } else {
      return -1;
    }
  }
}
