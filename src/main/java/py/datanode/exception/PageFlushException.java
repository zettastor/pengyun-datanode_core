
package py.datanode.exception;

public class PageFlushException extends Exception {
  private static final long serialVersionUID = 1L;

  public PageFlushException() {
    super();
  }

  public PageFlushException(String s) {
    super(s);
  }

  public PageFlushException(Throwable ex1) {
    super(ex1);
  }

  public PageFlushException(String s, Throwable ex1) {
    super(s, ex1);
  }
}
