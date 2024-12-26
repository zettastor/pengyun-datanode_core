
package py.datanode.exception;

public class TimedOutWaitingForAvailablePageException extends Exception {
  private static final long serialVersionUID = 1L;

  public TimedOutWaitingForAvailablePageException() {
    super();
  }

  public TimedOutWaitingForAvailablePageException(String message) {
    super(message);
  }

  public TimedOutWaitingForAvailablePageException(String message, Throwable cause) {
    super(message, cause);
  }

  public TimedOutWaitingForAvailablePageException(Throwable cause) {
    super(cause);
  }
}
