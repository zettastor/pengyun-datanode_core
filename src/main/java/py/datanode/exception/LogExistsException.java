

package py.datanode.exception;

public class LogExistsException extends Exception {
  private static final long serialVersionUID = 1L;

  public LogExistsException() {
    super();
  }

  public LogExistsException(String message) {
    super(message);
  }

  public LogExistsException(String message, Throwable cause) {
    super(message, cause);
  }

  public LogExistsException(Throwable cause) {
    super(cause);
  }
}
