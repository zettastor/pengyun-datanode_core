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

package py.datanode.storage.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.PriorityQueue;
import org.junit.Test;
import py.archive.page.PageAddress;
import py.datanode.page.Page;
import py.test.TestBase;

public class WritePageIoContextTest extends TestBase {
  @Test
  public void testCompare() {
    Page page1 = mock(Page.class);
    PageAddress address1 = mock(PageAddress.class);
    when(address1.getPhysicalOffsetInArchive()).thenReturn(1L);
    when(page1.getAddress()).thenReturn(address1);

    Page page2 = mock(Page.class);
    PageAddress address2 = mock(PageAddress.class);
    when(address2.getPhysicalOffsetInArchive()).thenReturn(2L);
    when(page2.getAddress()).thenReturn(address2);
    
    Integer int1 = 1;
    Integer int2 = 2;

    assertFalse(true ^ true);
    assertFalse(false ^ false);

    WritePageIoContext context1 = new WritePageIoContext(null, page1, true);
    assertTrue(int1.compareTo(int2) < 0);
    
    WritePageIoContext context2 = new WritePageIoContext(null, page2, true);
    assertTrue(context1.compareTo(context2) < 0);

    context1 = new WritePageIoContext(null, page1, false);
    context2 = new WritePageIoContext(null, page2, false);
    assertTrue(context1.compareTo(context2) < 0);

    context1 = new WritePageIoContext(null, page1, false);
    context2 = new WritePageIoContext(null, page2, true);
    assertTrue(context1.compareTo(context2) > 0);

    context1 = new WritePageIoContext(null, page1, true);
    context2 = new WritePageIoContext(null, page2, false);
    assertTrue(context1.compareTo(context2) < 0);

    context1 = new WritePageIoContext(null, page1, true);
    context2 = new WritePageIoContext(null, page2, false);
    assertTrue(context1.compareTo(context2) < 0);

    PriorityQueue<WritePageIoContext> queue = new PriorityQueue<>();
    queue.offer(context2);
    queue.offer(context1);

    assertEquals(context1, queue.poll());
  }

}