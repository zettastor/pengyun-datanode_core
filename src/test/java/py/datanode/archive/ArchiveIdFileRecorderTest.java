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
