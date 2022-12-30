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

package py.datanode.archive;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import py.datanode.configuration.DataNodeConfiguration;
import py.datanode.test.DataNodeConfigurationForTest;
import py.test.TestBase;

public class ArchiveIdFileRecorderTest extends TestBase {
  @Test
  public void testAddAndRemove() {
    DataNodeConfiguration cfg = new DataNodeConfigurationForTest();

    ArchiveIdFileRecorder.IMPROPERLY_EJECTED_RAW.init(cfg.getArchiveIdRecordPath());
    ArchiveIdFileRecorder.IMPROPERLY_EJECTED_RAW.add(Long.MAX_VALUE);
    assertTrue(ArchiveIdFileRecorder.IMPROPERLY_EJECTED_RAW.contains(Long.MAX_VALUE));

    ArchiveIdFileRecorder.IMPROPERLY_EJECTED_RAW.remove(Long.MAX_VALUE);
    assertTrue(!ArchiveIdFileRecorder.IMPROPERLY_EJECTED_RAW.contains(Long.MAX_VALUE));
  }
}
