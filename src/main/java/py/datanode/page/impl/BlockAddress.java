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

package py.datanode.page.impl;

import py.archive.segment.SegId;
import py.storage.Storage;

public class BlockAddress {
  public final long physicalOffset;

  public final Storage storage;
  private final SegId segId;

  public BlockAddress(Storage storage, long physicalOffset, SegId segId) {
    this.storage = storage;
    this.physicalOffset = physicalOffset;
    this.segId = segId;
  }

  @Override
  public String toString() {
    return "BlockAddress [hash=" + hashCode() + ", physicalOffset=" + physicalOffset + ", storage="
        + storage
        + ", segId=" + segId + "]";
  }

  public long getPhysicalOffset() {
    return physicalOffset;
  }

  public Storage getStorage() {
    return storage;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (int) (physicalOffset ^ (physicalOffset >>> 32));
    result = prime * result + ((storage == null) ? 0 : storage.hashCode());
    return result;
  }

  public SegId getSegId() {
    return segId;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    BlockAddress other = (BlockAddress) obj;
    if (physicalOffset != other.physicalOffset) {
      return false;
    }
    if (storage == null) {
      if (other.storage != null) {
        return false;
      }
    } else if (!storage.equals(other.storage)) {
      return false;
    } else if (segId == null) {
      if (other.getSegId() != null) {
        return false;
      }
    } else if (!segId.equals(other.getSegId())) {
      return false;
    }

    return true;
  }
}
