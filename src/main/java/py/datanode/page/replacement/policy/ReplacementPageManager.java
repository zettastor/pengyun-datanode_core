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

package py.datanode.page.replacement.policy;

public interface ReplacementPageManager<T> {
  /**
   * Remove a page to replacement page manager.
   */
  public boolean remove(T element);

  /**
   * Execute the algorithm to free a element, then someone can use the entry to save new data. If it
   * is called successfully, it will notify the {@link ReplacementPageManagerListener}.
   */
  public void free();

  /**
   * when you want to use a element, you should call this method, then replace algorithm will manage
   * the element for replacing.
   */
  public void visit(T element);

  /**
   * The number of element.
   */
  public int size();

  /**
   * Dump some information to logger.
   */
  public void dump();

  /**
   * clear all information in this manager.
   */
  public void clear();
}
