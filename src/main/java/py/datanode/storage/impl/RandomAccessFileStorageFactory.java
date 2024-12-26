
package py.datanode.storage.impl;

import py.exception.StorageException;
import py.storage.Storage;
import py.storage.impl.FileStorageFactory;

public class RandomAccessFileStorageFactory extends FileStorageFactory {
  @Override
  public Storage generate(String id) throws StorageException {
    return new RandomAccessFileStorage(id, file);
  }
}
