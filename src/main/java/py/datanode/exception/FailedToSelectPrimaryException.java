

package py.datanode.exception;

public class FailedToSelectPrimaryException extends Exception {
  public FailedToSelectPrimaryException() {
  }

  public FailedToSelectPrimaryException(String message) {
    super(message);
  }

  public FailedToSelectPrimaryException(String message, Throwable cause) {
    super(message, cause);
  }

  public FailedToSelectPrimaryException(Throwable cause) {
    super(cause);
  }

  public FailedToSelectPrimaryException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
