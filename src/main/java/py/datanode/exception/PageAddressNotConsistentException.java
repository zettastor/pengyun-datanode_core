
package py.datanode.exception;

import py.archive.page.PageAddress;

public class PageAddressNotConsistentException extends Exception {
  private static final long serialVersionUID = 1L;

  public PageAddressNotConsistentException() {
    super();
  }

  public PageAddressNotConsistentException(PageAddress oldAddress, PageAddress newAddress) {
    super("old PageAddress: " + oldAddress + " new PageAddress: " + newAddress);
  }

  public PageAddressNotConsistentException(String message) {
    super(message);
  }

  public PageAddressNotConsistentException(String message, Throwable cause) {
    super(message, cause);
  }

  public PageAddressNotConsistentException(Throwable cause) {
    super(cause);
  }
}
