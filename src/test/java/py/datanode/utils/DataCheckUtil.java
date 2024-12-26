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

package py.datanode.utils;

import java.nio.ByteBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataCheckUtil {
  private static final Logger logger = LoggerFactory.getLogger(DataCheckUtil.class);

  public static ByteBuffer buildByteBuffer(long offsetInArchive, int size) {
    ByteBuffer buffer = ByteBuffer.allocate(size);
    for (int i = 0; i < size / 8; i++) {
      buffer.putLong(offsetInArchive + i);
    }

    buffer.clear();
    return buffer;
  }

  public static void writeByteBuffer(long offsetInArchive, ByteBuffer buffer) {
    int size = buffer.remaining();
    for (int i = 0; i < size / 8; i++) {
      buffer.putLong(offsetInArchive + i);
    }
  }

  public static boolean checkByteBuffer(long offsetInArchive, ByteBuffer src) {
    for (int i = 0; i < src.remaining() / 8; i++) {
      long originValue = src.getLong();
      if (originValue != (offsetInArchive + i)) {
        logger.info("originValue: {}, expected: {}", originValue, offsetInArchive + i);
        return false;
      }
    }

    src.clear();
    return true;
  }

  public static boolean checkZero(ByteBuffer src) {
    for (int i = 0; i < src.remaining() / 8; i++) {
      if (src.getLong() != 0) {
        return false;
      }
    }

    src.clear();
    return true;
  }
}
