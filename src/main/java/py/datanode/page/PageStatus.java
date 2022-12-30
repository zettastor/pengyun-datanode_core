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

public enum PageStatus {
  FREE(0), READ(1), WRITE(2) {
    @Override
    public boolean canModify() {
      return true;
    }
  };

  private final int value;

  private PageStatus(int value) {
    this.value = value;
  }

  public int getValue() {
    return value;
  }

  public PageStatus findByValue(int value) {
    switch (value) {
      case 0:
        return PageStatus.FREE;
      case 1:
        return PageStatus.READ;
      case 2:
        return PageStatus.WRITE;
      default:
        throw new IllegalArgumentException("not support the value: " + value);
    }
  }

  public boolean canModify() {
    return false;
  }
}
