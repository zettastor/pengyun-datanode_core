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
