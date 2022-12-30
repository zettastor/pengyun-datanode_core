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
