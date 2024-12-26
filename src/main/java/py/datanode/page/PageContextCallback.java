

package py.datanode.page;

public interface PageContextCallback<T extends Page> {
  public void completed(PageContext<T> pageContext);
}
