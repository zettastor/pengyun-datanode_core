

package py.datanode.exception;

public class DataLossException extends Exception {
  private static final long serialVersionUID = 1L;

  public DataLossException() {
    super();
  }

  public DataLossException(String message) {
    super(message);
  }

  public DataLossException(String message, Throwable cause) {
    super(message, cause);
  }

  public DataLossException(Throwable cause) {
    super(cause);
  }

}
