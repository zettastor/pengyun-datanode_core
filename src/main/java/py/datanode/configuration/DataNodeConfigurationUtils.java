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
