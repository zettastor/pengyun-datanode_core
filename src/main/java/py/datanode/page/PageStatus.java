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
