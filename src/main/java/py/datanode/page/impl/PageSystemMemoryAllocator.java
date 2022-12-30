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

package py.datanode.page.impl;

import java.lang.management.ManagementFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.informationcenter.Utils;

public class PageSystemMemoryAllocator {
  private static final Logger logger = LoggerFactory.getLogger(PageSystemMemoryAllocator.class);

  public static long getPageSystemMemoryCacheSize() {
    @SuppressWarnings("restriction")
    long memorySize =
        ((com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean())
            .getTotalPhysicalMemorySize() / 1024;

    String cacheSize = "100M";
    if (memorySize >= 200000000) {
      cacheSize = "85G";
    } else if (memorySize >= 120000000) {
      cacheSize = "67G";
    } else if (memorySize >= 60000000) {
      cacheSize = "23G";
    } else if (memorySize >= 30000000) {
      cacheSize = "17G";
    } else if (memorySize >= 14000000) {
      cacheSize = "8G";
    } else if (memorySize >= 7000000) {
      cacheSize = "3G";
    } else if (memorySize >= 3000000) {
      cacheSize = "200M";
    }

    logger.warn("page system memory size: {}, system memory: {}", cacheSize, memorySize);
    return Utils.getByteSize(cacheSize);
  }
}
