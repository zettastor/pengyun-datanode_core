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
