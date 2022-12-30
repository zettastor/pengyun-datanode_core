/*
 * Copyright (c) 2022. PengYunNetWork
 *
 * This program is free software: you can use, redistribute, and/or modify it
 * under the terms of the GNU Affero General Public License, version 3 or later ("AGPL"),
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 *  You should have received a copy of the GNU Affero General Public License along with
 *  this program. If not, see <http://www.gnu.org/licenses/>.
 */

package py.datanode.exception;

import java.nio.file.Path;

public class BadLogFileException extends Exception {
  private static final long serialVersionUID = 1L;
  private Path badFile;
  private long newSize;

  public BadLogFileException(Path file, String errMsg) {
    super(errMsg);
    this.badFile = file;
    this.newSize = 0;
  }

  public BadLogFileException(Path file, String errMsg, Throwable e) {
    super(errMsg, e);
    this.badFile = file;
    this.newSize = 0;
  }

  public BadLogFileException(Path file, String errMsg, long newSize) {
    super(errMsg);
    this.badFile = file;
    this.newSize = newSize;
  }

  public BadLogFileException(Path file, String errMsg, long newSize, Throwable e) {
    super(errMsg, e);
    this.badFile = file;
    this.newSize = newSize;
  }

  public Path getBadFile() {
    return badFile;
  }

  public long getNewSize() {
    return newSize;
  }

  public void setNewSize(long newSize) {
    this.newSize = newSize;
  }

}
