
package py.datanode.page;

public interface PageIoListener {
  void loadedFromStorage(PageContext<Page> context);

  void flushedToStorage(PageContext<Page> context);

  void flushedToL2Write(PageContext<Page> context);
}
