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

package py.datanode.configuration;

import py.archive.ArchiveType;
import py.exception.NotSupportedException;

public class DataNodeConfigurationUtils {
  public static ArchiveType getArchiveTypeByDirName(DataNodeConfiguration cfg, String dirName)
      throws NotSupportedException {
    if (cfg.getArchiveConfiguration().getDataArchiveDir().compareToIgnoreCase(dirName) == 0) {
      return ArchiveType.RAW_DISK;
    } else if (cfg.getArchiveConfiguration().getUnsettledArchiveDir().compareToIgnoreCase(dirName)
        == 0) {
      return ArchiveType.UNSETTLED_DISK;
    } else {
      throw new NotSupportedException("not support the dir=" + dirName);
    }
  }

  public static String getDirNameByArchiveType(DataNodeConfiguration cfg, ArchiveType archiveType)
      throws NotSupportedException {
    if (ArchiveType.RAW_DISK.equals(archiveType)) {
      return cfg.getArchiveConfiguration().getDataArchiveDir();
    } else if (ArchiveType.UNSETTLED_DISK.equals(archiveType)) {
      return cfg.getArchiveConfiguration().getUnsettledArchiveDir();
    } else {
      throw new NotSupportedException("not support the dir=" + archiveType);
    }
  }

  public static String getDataDir(DataNodeConfiguration cfg) {
    return null;
  }
}
