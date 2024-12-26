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
