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
