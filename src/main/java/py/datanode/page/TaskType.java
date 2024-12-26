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

import org.apache.directory.api.util.exception.NotImplementedException;
import py.exception.StorageException;

public enum TaskType {
  
  CHECK_OUT_FOR_READ(0) {
    @Override
    public TaskType getCheckinType() {
      return TaskType.CHECK_IN_FOR_READ;
    }
  },

  CHECK_OUT_FOR_INTERNAL_WRITE(1) {
    @Override
    public TaskType getCheckinType() {
      return TaskType.CHECK_IN_FOR_INTERNAL_WRITE;
    }
  },

  CHECK_OUT_FOR_INTERNAL_CORRECTION(2) {
    @Override
    public TaskType getCheckinType() {
      return TaskType.CHECK_IN_FOR_INTERNAL_CORRECTION;
    }
  },

  CHECK_OUT_FOR_EXTERNAL_READ(3) {
    @Override
    public boolean isCachedForRead() {
      return true;
    }

    @Override
    public TaskType getCheckinType() {
      return TaskType.CHECK_IN_FOR_EXTERNAL_READ;
    }
  },

  CHECK_IN_FOR_READ(4) {
    @Override
    public boolean isPageCheckIn() {
      return true;
    }
  },

  CHECK_IN_FOR_INTERNAL_WRITE(5) {
    @Override
    public boolean isPageCheckIn() {
      return true;
    }
  },

  CHECK_IN_FOR_INTERNAL_CORRECTION(6) {
    @Override
    public boolean isPageCheckIn() {
      return true;
    }
  },

  CHECK_IN_FOR_EXTERNAL_READ(7) {
    @Override
    public boolean isCachedForRead() {
      return true;
    }

    @Override
    public boolean isPageCheckIn() {
      return true;
    }
  },

  LOADED_FROM_DISK(8),

  FLUSHED_TO_DISK(9),

  CLEAN_CACHE(10),

  FLUSH_PAGE(11),

  CHECK_OUT_FOR_EXTERNAL_WRITE(12) {
    @Override
    public TaskType getCheckinType() {
      return CHECK_IN_FOR_EXTERNAL_WRITE;
    }
  },
  CHECK_OUT_FOR_EXTERNAL_CORRECTION(13) {
    @Override
    public TaskType getCheckinType() {
      return CHECK_IN_FOR_EXTERNAL_CORRECTION;
    }
  },
  CHECK_IN_FOR_EXTERNAL_WRITE(14) {
    @Override
    public boolean isPageCheckIn() {
      return true;
    }
  },
  CHECK_IN_FOR_EXTERNAL_CORRECTION(15) {
    @Override
    public boolean isPageCheckIn() {
      return true;
    }
  };
  private final int value;

  TaskType(int value) {
    this.value = value;
  }

  public static TaskType findByValue(int value) {
    switch (value) {
      case 0:
        return TaskType.CHECK_OUT_FOR_READ;
      case 1:
        return TaskType.CHECK_OUT_FOR_INTERNAL_WRITE;
      case 2:
        return TaskType.CHECK_OUT_FOR_INTERNAL_CORRECTION;
      case 3:
        return TaskType.CHECK_OUT_FOR_EXTERNAL_READ;
      case 4:
        return TaskType.CHECK_IN_FOR_READ;
      case 5:
        return TaskType.CHECK_IN_FOR_INTERNAL_WRITE;
      case 6:
        return TaskType.CHECK_IN_FOR_INTERNAL_CORRECTION;
      case 7:
        return TaskType.CHECK_IN_FOR_EXTERNAL_READ;
      case 8:
        return TaskType.LOADED_FROM_DISK;
      case 9:
        return TaskType.FLUSHED_TO_DISK;
      case 10:
        return TaskType.CLEAN_CACHE;
      case 11:
        return TaskType.FLUSH_PAGE;
      case 12:
        return TaskType.CHECK_OUT_FOR_EXTERNAL_WRITE;
      case 13:
        return TaskType.CHECK_OUT_FOR_EXTERNAL_CORRECTION;
      case 14:
        return TaskType.CHECK_IN_FOR_EXTERNAL_WRITE;
      case 15:
        return TaskType.CHECK_IN_FOR_EXTERNAL_CORRECTION;
      default:
        return null;
    }
  }

  public int getValue() {
    return value;
  }

  public TaskType getCheckinType() {
    throw new NotImplementedException("not support the method");
  }

  public boolean isCachedForRead() {
    return false;
  }

  public boolean isPageCheckIn() {
    return false;
  }
}
