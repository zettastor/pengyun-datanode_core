
package py.datanode.page;

public interface PageListener {
  /**
   * When the page becomes clean, we should receive the notification.
   */
  public void successToPersist();

  /**
   * If the page can not be flushed to disk, the method be called to tell the user why ?.
   */
  public void failToPersist(Exception e);
}
