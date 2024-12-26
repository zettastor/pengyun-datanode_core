

package py.datanode.exception;

public class LogMergingException extends Exception {
  private static final long serialVersionUID = 1L;

  public LogMergingException() {
    super();
  }

  public LogMergingException(String message) {
    super(message);
  }

  public LogMergingException(String message, Throwable cause) {
    super(message, cause);
  }

  public LogMergingException(Throwable cause) {
    super(cause);
  }
}
