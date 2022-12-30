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
