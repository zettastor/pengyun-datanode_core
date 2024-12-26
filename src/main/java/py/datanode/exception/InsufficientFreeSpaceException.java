
package py.datanode.exception;

public class InsufficientFreeSpaceException extends Exception {
  private static final long serialVersionUID = 1L;

  public InsufficientFreeSpaceException(String s) {
    super(s);
  }

  public InsufficientFreeSpaceException(Throwable ex1) {
    super(ex1);
  }

  public InsufficientFreeSpaceException(String s, Throwable ex1) {
    super(s, ex1);
  }
}
