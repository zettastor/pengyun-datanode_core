
package py.datanode.exception;

import py.exception.StorageException;

public class CacheStorageIoException extends StorageException {
  public CacheStorageIoException(Throwable ex1) {
    super(ex1);
  }

  @Override
  public String toString() {
    return "CacheStorageIOException{super" + super.toString() + '}';
  }
}
