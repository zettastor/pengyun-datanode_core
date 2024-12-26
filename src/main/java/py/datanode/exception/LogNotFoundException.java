

package py.datanode.exception;

public class LogNotFoundException extends Exception {
  private static final long serialVersionUID = 1L;

  public LogNotFoundException() {
    super();
  }

  public LogNotFoundException(String message) {
    super(message);
  }

  public LogNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }

  public LogNotFoundException(Throwable cause) {
    super(cause);
  }
}
