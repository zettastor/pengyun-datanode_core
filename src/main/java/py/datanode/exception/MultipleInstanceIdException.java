
package py.datanode.exception;

public class MultipleInstanceIdException extends Exception {
  private static final long serialVersionUID = 1L;

  public MultipleInstanceIdException(String s) {
    super(s);
  }

  public MultipleInstanceIdException(Throwable ex1) {
    super(ex1);
  }

  public MultipleInstanceIdException(String s, Throwable ex1) {
    super(s, ex1);
  }
}
