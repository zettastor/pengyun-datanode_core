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

package py.datanode.page;

import org.junit.Assert;
import org.junit.Test;
import py.archive.ArchiveOptions;
import py.archive.ArchiveType;
import py.archive.page.PageAddress;
import py.common.tlsf.bytebuffer.manager.TlsfByteBufferManagerFactory;
import py.datanode.configuration.DataNodeConfiguration;
import py.datanode.page.impl.PageAddressGenerator;
import py.datanode.storage.impl.StorageBuilder;
import py.datanode.test.DataNodeConfigurationForTest;
import py.storage.Storage;
import py.test.TestBase;

public class PageAddressGeneratorTest extends TestBase {
  private static final int FLEXIBLE_LIMIT_IN_ONE_ARCHIVE = 20;
  protected int segmentCount = 10;

  @Test
  public void generatorShadowPagesTest() throws Exception {
    DataNodeConfiguration cfg = new DataNodeConfigurationForTest();
    cfg.setPageSize(8192);
    cfg.setSegmentUnitSize(16 * 1024 * 1024);
    TlsfByteBufferManagerFactory.init(512, 1024 * 1024 * 10, true);
    cfg.setFlexibleCountLimitInOneArchive(FLEXIBLE_LIMIT_IN_ONE_ARCHIVE);
    ArchiveOptions.initContants(cfg.getPageSize(), cfg.getSegmentUnitSize(),
        cfg.getFlexibleCountLimitInOneArchive());
    Assert.assertEquals(ArchiveOptions.SEGMENTUNIT_DESCDATA_LENGTH,
        ArchiveOptions.SEGMENTUNIT_METADATA_LENGTH + ArchiveOptions.SEGMENTUNIT_BITMAP_LENGTH
            + ArchiveOptions.SEGMENTUNIT_ACCEPTOR_LENGTH);
    long storageSize = 0;
    // archive meta data length
    storageSize += (long) ArchiveType.RAW_DISK.getArchiveHeaderLength();
    // all arbiters length
    storageSize += ArchiveOptions.ALL_FLEXIBLE_LENGTH;

    storageSize += segmentCount * ArchiveOptions.BRICK_DESCDATA_LENGTH;

    storageSize = ((storageSize + ArchiveOptions.SEGMENT_UNIT_DATA_ALIGNED_SIZE - 1)
        / ArchiveOptions.SEGMENT_UNIT_DATA_ALIGNED_SIZE)
        * ArchiveOptions.SEGMENT_UNIT_DATA_ALIGNED_SIZE;

    storageSize +=
        ((segmentCount) * (ArchiveOptions.SEGMENT_PHYSICAL_SIZE));

    Storage storage = StorageBuilder.getDummyStorage((int) storageSize);

    long offsetInArchive = 431742976L;
    long offsetSegment = 3923968L;

    PageAddress pageAddress = PageAddressGenerator
        .generateAddressOfShadowPage(offsetInArchive, offsetSegment, storage);
    Assert.assertEquals(0, pageAddress.getOffsetInSegment());
  }
}
