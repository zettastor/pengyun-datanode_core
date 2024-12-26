

package py.datanode.exception;

public class InappropriateLogStatusException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public InappropriateLogStatusException() {
    super();
  }

  public InappropriateLogStatusException(String message) {
    super(message);
  }

  public InappropriateLogStatusException(String message, Throwable cause) {
    super(message, cause);
  }

  public InappropriateLogStatusException(Throwable cause) {
    super(cause);
  }
}
