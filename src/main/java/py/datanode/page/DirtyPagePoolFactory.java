

package py.datanode.page;

import py.storage.Storage;

public interface DirtyPagePoolFactory {
  DirtyPagePool getOrBuildDirtyPagePool(Storage storage);
}
