

package py.datanode.exception;

public class GenericPageSystemException extends Exception {
  private static final long serialVersionUID = -1406952933038777767L;

  public GenericPageSystemException() {
    super();
  }

  public GenericPageSystemException(String message) {
    super(message);
  }

  public GenericPageSystemException(String message, Throwable cause) {
    super(message, cause);
  }

  public GenericPageSystemException(Throwable cause) {
    super(cause);
  }

}
