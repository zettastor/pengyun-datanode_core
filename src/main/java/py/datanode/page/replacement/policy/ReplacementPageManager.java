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
