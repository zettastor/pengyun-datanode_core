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

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileOutputStream;
import org.junit.Test;
import py.datanode.storage.impl.RandomAccessFileStorage;
import py.datanode.storage.impl.RandomAccessFileStorageFactory;
import py.test.TestBase;

public class TestDisk extends TestBase {
  @Test
  public void testGetSize() throws Exception {
    // Create a file first
    int unitSize = 1024;
    // 16MB;
    int ntimes = 1024 * 16;
    byte[] dataToWrite = new byte[unitSize];

    String strFileName = "/tmp/testDisk";
    File file = new File(strFileName);

    FileOutputStream out = new FileOutputStream(file);
    for (int i = 0; i < ntimes; i++) {
      out.write(dataToWrite);
    }
    out.close();

    RandomAccessFileStorageFactory factory = new RandomAccessFileStorageFactory();
    factory.setFile(file);
    RandomAccessFileStorage rafs = (RandomAccessFileStorage) factory.generate("aaaa");
    assertEquals((long) unitSize * (long) ntimes, rafs.size());
    rafs.close();
  }
}
