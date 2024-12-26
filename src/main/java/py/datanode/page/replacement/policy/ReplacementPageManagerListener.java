
package py.datanode.page.replacement.policy;

/**
 * It is used by the replacement algorithm.
 */
public interface ReplacementPageManagerListener<T> {
  public boolean canEvict(T element);

  public void evict(T element);

}
