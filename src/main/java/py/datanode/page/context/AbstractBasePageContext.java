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

package py.datanode.page.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.datanode.page.Page;
import py.datanode.page.TaskType;
import py.engine.BogusLatency;
import py.engine.Latency;

public abstract class AbstractBasePageContext<P extends Page> extends ComparablePageContext<P> {
  private static final Logger logger = LoggerFactory.getLogger(AbstractBasePageContext.class);
 
  private final String name;
  private TaskType taskType;
  private Exception exception;
  private Latency latency = BogusLatency.DEFAULT;

  public AbstractBasePageContext(TaskType taskType) {
    this.name = Thread.currentThread().getName();
    this.taskType = taskType;
  }

  @Override
  public Latency getLatency() {
    return latency;
  }

  @Override
  public void setLatency(Latency latency) {
    this.latency = latency;
  }

  @Override
  public void done() {
    logger.warn("context: not support done {} {}", getClass(), toString());
  }

  @Override
  public TaskType getTaskType() {
    return taskType;
  }

  @Override
  public void setTaskType(TaskType taskType) {
    this.taskType = taskType;
  }

  @Override
  public Exception getCause() {
    return exception;
  }

  @Override
  public void setCause(Exception e) {
    this.exception = e;
  }

  @Override
  public boolean isSuccess() {
    return exception == null;
  }

  @Override
  public String toString() {
    return "BasePageContextImpl [taskType=" + taskType + ", hashCode=" + hashCode() + ", name="
        + name + ", e=" + exception
        + "]";
  }

}
