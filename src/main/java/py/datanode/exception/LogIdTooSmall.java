

package py.datanode.exception;

public class LogIdTooSmall extends Exception {
  private static final long serialVersionUID = 1L;
  private long currentSmallestId;

  public LogIdTooSmall() {
    super();
  }

  public LogIdTooSmall(String message) {
    super(message);
  }

  public LogIdTooSmall(String message, Throwable cause) {
    super(message, cause);
  }

  public LogIdTooSmall(Throwable cause) {
    super(cause);
  }

  public long getCurrentSmallestId() {
    return currentSmallestId;
  }

  public LogIdTooSmall setCurrentSmallestId(long clId) {
    this.currentSmallestId = clId;
    return this;
  }

}
