
package py.datanode.storage.impl;

import py.exception.StorageException;
import py.storage.Storage;
import py.storage.impl.FileStorageFactory;

public class PageAlignedStorageFactory extends FileStorageFactory {
  private String mode = "rw";

  public String getMode() {
    return this.mode;
  }

  public void setMode(String mode) {
    this.mode = mode;
  }

  @Override
  public Storage generate(String id) throws StorageException {
    return new PageAlignedStorage(id, file, mode);
  }
}
