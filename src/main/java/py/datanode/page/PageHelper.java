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

import py.archive.segment.SegId;
import py.datanode.page.context.AsyncShadowPageContextImpl;
import py.datanode.page.context.BogusPageContext;

public class PageHelper {
  public static void shadow(AsyncShadowPageContextImpl<Page> context) {
    Page shadowPage = context.getShadowPageContext().getPage();
    shadowPage.write(0, context.getOriginalPageContext().getPage().getReadOnlyView());
    shadowPage.setPageLoaded(true);
  }

  public static void checkIn(PageManager<Page> pageManager, PageContext<Page> context,
      SegId segId) {
    if (context instanceof AsyncShadowPageContextImpl) {
      AsyncShadowPageContextImpl<Page> contextImpl = (AsyncShadowPageContextImpl<Page>) context;
      contextImpl.getOriginalPageContext().updateSegId(segId);
      pageManager.checkin(contextImpl.getOriginalPageContext());
      if (!(contextImpl.getShadowPageContext() instanceof BogusPageContext)) {
        contextImpl.getShadowPageContext().updateSegId(segId);
        pageManager.checkin(contextImpl.getShadowPageContext());
      }
    } else {
      if (context.isSuccess()) {
        context.updateSegId(segId);
      }
      pageManager.checkin(context);
    }
  }
}
