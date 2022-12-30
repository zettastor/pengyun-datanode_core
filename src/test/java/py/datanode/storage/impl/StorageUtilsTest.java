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

package py.datanode.storage.impl;

import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;
import py.exception.StorageException;
import py.storage.Storage;
import py.storage.impl.StorageUtils;
import py.test.TestBase;

public class StorageUtilsTest extends TestBase {
  public StorageUtilsTest() throws Exception {
    super.init();
  }

  @Test
  public void pattern() {
    Pattern rawPattern = Pattern.compile("(.*/dev/)((\\w+))", Pattern.CASE_INSENSITIVE);
    Matcher matcher = rawPattern.matcher("nbd-client 10.0.1.18 1234 /dev/Pynbd0");
    if (matcher.find()) {
      for (int i = 0; i < matcher.groupCount(); i++) {
        logger.info("i: {}, group: {}", i, matcher.group(i));
      }
    }
    logger.info("group count: {}, match: {}", matcher.groupCount(), matcher.matches());
  }

  @Test
  public void baseMatch() {
    Storage storage = generateStorage("c:/fff/ggg/mm/raw1");
    assertTrue(StorageUtils.isSata(storage));
    assertTrue(!StorageUtils.isSsd(storage));
    assertTrue(!StorageUtils.isPcie(storage));

    storage = generateStorage("c:/fff/ggg/mm/Raw1");
    assertTrue(StorageUtils.isSata(storage));
    assertTrue(!StorageUtils.isSsd(storage));
    assertTrue(!StorageUtils.isPcie(storage));

    storage = generateStorage("c:/fff/ggg/mm/ssd10");
    assertTrue(!StorageUtils.isSata(storage));
    assertTrue(StorageUtils.isSsd(storage));
    assertTrue(!StorageUtils.isPcie(storage));

    storage = generateStorage("c:/fff/ggg/mm/sSd10");
    assertTrue(!StorageUtils.isSata(storage));
    assertTrue(StorageUtils.isSsd(storage));
    assertTrue(!StorageUtils.isPcie(storage));

    storage = generateStorage("c:/fff/ggg/mm/pcie100");
    assertTrue(!StorageUtils.isSata(storage));
    assertTrue(!StorageUtils.isSsd(storage));
    assertTrue(StorageUtils.isPcie(storage));

    storage = generateStorage("c:/fff/ggg/mm/PCIE10");
    assertTrue(!StorageUtils.isSata(storage));
    assertTrue(!StorageUtils.isSsd(storage));
    assertTrue(StorageUtils.isPcie(storage));
  }

  @Test
  public void baseNotMatch() {
    Storage storage = generateStorage("c:/fff/ggg/mm/rawssd10");
    assertTrue(!StorageUtils.isSata(storage));
    assertTrue(!StorageUtils.isSsd(storage));
    assertTrue(!StorageUtils.isPcie(storage));

    storage = generateStorage("c:/fff/ggg/mm/pci10");
    assertTrue(!StorageUtils.isSata(storage));
    assertTrue(!StorageUtils.isSsd(storage));
    assertTrue(!StorageUtils.isPcie(storage));
  }

  public Storage generateStorage(String identifier) {
    return new Storage(identifier) {
      @Override
      public void read(long pos, ByteBuffer buffer) throws StorageException {
       

      }
      
      @Override
      public void read(long pos, byte[] dstBuf, int off, int len) throws StorageException {
       

      }

      @Override
      public void write(long pos, byte[] buf, int off, int len) throws StorageException {
       

      }

      @Override
      public void write(long pos, ByteBuffer buffer) throws StorageException {
       

      }

      @Override
      public long size() {
       
        return 0;
      }

    };
  }
}
