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
