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
