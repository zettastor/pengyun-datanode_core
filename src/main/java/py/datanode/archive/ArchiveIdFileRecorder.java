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

package py.datanode.archive;

import py.common.file.SimpleFileRecorder;

public enum ArchiveIdFileRecorder {
  IMPROPERLY_EJECTED_RAW;

  private SimpleFileRecorder fileRecorder;

  public synchronized void init(String filePath) {
    fileRecorder = new SimpleFileRecorder(filePath + name());
  }

  public synchronized boolean contains(long archiveId) {
    return fileRecorder.contains(String.valueOf(archiveId));
  }

  public synchronized boolean add(long archiveId) {
    return fileRecorder.add(String.valueOf(archiveId));
  }

  public synchronized boolean remove(long archiveId) {
    return fileRecorder.remove(String.valueOf(archiveId));
  }

  public synchronized boolean isEmpty() {
    return fileRecorder.isEmpty();
  }
}
