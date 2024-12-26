
package py.datanode.exception;

import java.nio.file.Path;

public class EmptyLogFileException extends Exception {
  private static final long serialVersionUID = 1L;
  private Path badFile;

  public EmptyLogFileException(Path file, String errMsg) {
    super(errMsg);
    this.badFile = file;
  }

  public Path getBadFile() {
    return badFile;
  }
}
