

package py.datanode.archive;

import py.archive.ArchiveStatusListener;
import py.archive.PluginPlugoutManager;

public interface UnsettledArchiveManager extends PluginPlugoutManager, ArchiveStatusListener {
  void removeArchive(long archiveId);

}
