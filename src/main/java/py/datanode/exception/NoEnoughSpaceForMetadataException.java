

package py.datanode.exception;

public class NoEnoughSpaceForMetadataException extends Exception {
  private static final long serialVersionUID = 1L;

  public NoEnoughSpaceForMetadataException(String s) {
    super(s);
  }

  public NoEnoughSpaceForMetadataException(Throwable ex1) {
    super(ex1);
  }

  public NoEnoughSpaceForMetadataException(String s, Throwable ex1) {
    super(s, ex1);
  }

}
