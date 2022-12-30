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

package py.datanode.exception;

import py.archive.segment.SegId;

public class SegmentUnitPlacementException extends Exception {
  private static final long serialVersionUID = 1L;

  public SegmentUnitPlacementException(String archive, SegId segId, long size) {
    super(
        "No space on archive " + archive + " for volume " + segId.toString() + " of size " + size);
  }

  public SegmentUnitPlacementException(SegId segId, long size) {
    super(
        "Could not find space on any archive for volume " + segId.toString() + " of size " + size);
  }
}
