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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashSet;
import java.util.Set;
import org.junit.Ignore;

public class TestMemoryMapping {
  @Ignore
  public static void main(String[] args) {
    System.out.println(Runtime.getRuntime().availableProcessors() * 2);

    if (args.length != 3) {
      System.out.println(" TestMemoryMapping mmpp_size mmpps_size");
      System.exit(1);
    }
    TestMemoryMapping mapping = new TestMemoryMapping();
    File file = new File("/tmp/mapping_file");
    try {
      mapping.makeMemoryMappedPages(file, 16 * 1024, 24576 * 8);
    } catch (Exception e) {
     
      e.printStackTrace();
    }
  }

  @SuppressWarnings("resource")
  @Ignore
  public void makeMemoryMappedPages(File file, int pageSize, long numPages) throws Exception {
    FileChannel channel = null;
    Set<ByteBuffer> buffers = new HashSet<ByteBuffer>();
    try {
      byte[] zeroes = new byte[pageSize];
      channel = new RandomAccessFile(file, "rw").getChannel();
      for (long i = 0; i < numPages; i++) {
        ByteBuffer buffer = channel.map(FileChannel.MapMode.READ_WRITE, i * pageSize, pageSize);
        buffer.put(zeroes);
        buffers.add(buffer);
      }
      System.out.println("done");
    } catch (IOException e) {
      e.printStackTrace();
      System.out.println("caught an io exception" + e.getMessage());
      throw new Exception(e);
    } finally {
      try {
        channel.close();
      } catch (Exception e) {
        e.printStackTrace();
        System.out.println("caught an exception" + e.getMessage());
      }
    }
  }
}
