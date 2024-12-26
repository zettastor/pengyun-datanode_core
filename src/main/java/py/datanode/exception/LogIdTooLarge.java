
package py.datanode.exception;

public class LogIdTooLarge extends Exception {
  private static final long serialVersionUID = 1L;

  public LogIdTooLarge() {
    super();
  }

  public LogIdTooLarge(String message) {
    super(message);
  }

  public LogIdTooLarge(String message, Throwable cause) {
    super(message, cause);
  }

  public LogIdTooLarge(Throwable cause) {
    super(cause);
  }
}
