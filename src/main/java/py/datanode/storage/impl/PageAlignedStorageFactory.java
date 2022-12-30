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

package py.datanode.storage.impl;

import py.exception.StorageException;
import py.storage.Storage;
import py.storage.impl.FileStorageFactory;

public class PageAlignedStorageFactory extends FileStorageFactory {
  private String mode = "rw";

  public String getMode() {
    return this.mode;
  }

  public void setMode(String mode) {
    this.mode = mode;
  }

  @Override
  public Storage generate(String id) throws StorageException {
    return new PageAlignedStorage(id, file, mode);
  }
}
