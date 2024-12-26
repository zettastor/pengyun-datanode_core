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

public enum IoType {
  
  TODISK(0) {
    @Override
    public boolean isRead() {
      return false;
    }
  },

  FROMDISK(1) {
    @Override
    public boolean isRead() {
      return true;
    }
  },

  @Deprecated
  FROML2(2) {
    @Override
    public boolean isRead() {
      return true;
    }
  },

  @Deprecated
  TOL2(3) {
    @Override
    public boolean isRead() {
      return false;
    }
  },

  @Deprecated
  CLEANL2(4) {
    @Override
    public boolean isRead() {
      return false;
    }
  },
  @Deprecated
  CLEANL2METADATA(5) {
    @Override
    public boolean isRead() {
      return false;
    }
  },

  TODISKFOREXTERNAL(6) {
    @Override
    public boolean isRead() {
      return false;
    }

    @Override
    public boolean isExternal() {
      return true;
    }
  },

  FROMDISKFOREXTERNAL(7) {
    @Override
    public boolean isRead() {
      return true;
    }

    @Override
    public boolean isExternal() {
      return true;
    }
  };

  private final int value;

  private IoType(int value) {
    this.value = value;
  }

  public static IoType findByValue(int value) {
    switch (value) {
      case 0:
        return TODISK;
      case 1:
        return FROMDISK;
      case 2:
        return FROML2;
      case 3:
        return TOL2;
      case 4:
        return CLEANL2;
      case 5:
        return CLEANL2METADATA;
      case 6:
        return TODISKFOREXTERNAL;
      case 7:
        return FROMDISKFOREXTERNAL;
      default:
        return null;
    }
  }

  public int getValue() {
    return value;
  }

  public boolean isRead() {
    return false;
  }

  public boolean isExternal() {
    return false;
  }
}
