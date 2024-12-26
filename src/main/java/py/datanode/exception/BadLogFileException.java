/**
* Copyright (C) 2013-2024 Nanjing Pengyun Network Technology Co., Ltd.
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
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
