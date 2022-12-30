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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.datanode.page.Page;
import py.datanode.page.PageListener;
import py.datanode.page.PageStatus;

public abstract class AbstractPage implements Page {
  private static final Logger logger = LoggerFactory.getLogger(AbstractPage.class);

  private final AtomicInteger checkoutTimes;

  private final AtomicBoolean flushing;

  private volatile PageStatus pageStatus;

  private volatile boolean isPageLoaded;
  /**
   * Not all read pages should be cached in Page system, only read request from coordinator
   * service.
   */
  private volatile boolean isCachedForRead;
  private volatile boolean dirty;
  private volatile boolean clean;

  private List<PageListener> pageListeners;

  public AbstractPage() {
    this.checkoutTimes = new AtomicInteger(0);
    this.flushing = new AtomicBoolean(false);
    this.pageStatus = PageStatus.FREE;
    this.isPageLoaded = false;
    this.isCachedForRead = false;
    this.dirty = false;
    this.clean = false;
    this.pageListeners = new ArrayList<PageListener>();
  }

  @Override
  public boolean isDirty() {
    return dirty;
  }

  @Override
  public void setDirty(boolean isDirty) {
    this.dirty = isDirty;
    this.clean = false;
  }

  @Override
  public int checkout() {
    return checkoutTimes.incrementAndGet();
  }

  @Override
  public int checkin() {
    int value = checkoutTimes.decrementAndGet();
    if (value >= 0) {
      return value;
    } else {
      logger.error("it is incredible checkoutTimes: {} on page: {}", value, this);
      return value;
    }
  }

  public ByteBuffer getDataBuffer() {
    throw new NotImplementedException("this is a PageImpl");
  }


  public void getData(int pageOffset, byte[] dst, int offset, int length) {
    throw new NotImplementedException("this is a PageImpl");
  }

  @Override
  public void getData(int pageOffset, ByteBuffer byteBuffer) {
    throw new NotImplementedException("this is a PageImpl");
  }

  @Override
  public int getCheckoutCount() {
    return checkoutTimes.get();
  }

  @Override
  public boolean isFlushing() {
    return flushing.get();
  }

  @Override
  public boolean canRead() {
    if (pageStatus == PageStatus.FREE) {
      pageStatus = PageStatus.READ;
      return true;
    } else if (pageStatus == PageStatus.READ) {
      return true;
    } else {
      return false;
    }
  }

  @Override
  public boolean canWrite() {
    if (pageStatus == PageStatus.FREE) {
      if (flushing.compareAndSet(false, true)) {
        pageStatus = PageStatus.WRITE;
        return true;
      } else {
        return false;
      }
    } else {
      return false;
    }
  }

  @Override
  public boolean canFlush() {
    return flushing.compareAndSet(false, true);
  }

  public PageStatus getPageStatus() {
    return pageStatus;
  }

  public void setPageStatus(PageStatus pageStatus) {
    this.pageStatus = pageStatus;
  }

  @Override
  public void setCanbeFlushed() {
    if (!flushing.getAndSet(false)) {
      logger.info("why call this method and nobody flushs the page: {}", this);
    }
  }

  @Override
  public boolean isCachedForRead() {
    return isCachedForRead;
  }

  @Override
  public void setCachedForRead(boolean isCachedForRead) {
    this.isCachedForRead = isCachedForRead;
  }

  @Override
  public boolean isPageLoaded() {
    return this.isPageLoaded;
  }

  @Override
  public void setPageLoaded(boolean isPageLoaded) {
    this.isPageLoaded = isPageLoaded;
  }

  @Override
  public void addPageListener(PageListener pageListener) {
    this.pageListeners.add(pageListener);
  }

  @Override
  public List<PageListener> getPageListeners() {
    return pageListeners;
  }

  @Override
  public void removeListeners() {
    pageListeners.clear();
  }

  @Override
  public boolean isClean() {
    return clean;
  }

  @Override
  public void setClean(boolean clean) {
    this.clean = clean;
  }

  @Override
  public String toString() {
    return "AbstractPage [hashCode=" + hashCode() + ", checkoutTimes=" + checkoutTimes
        + ", pageStatus="
        + pageStatus + ", flushing=" + flushing + ", isCachedForRead=" + isCachedForRead
        + ", isPageLoaded="
        + isPageLoaded + ", isDirty=" + dirty + ", clean=" + clean + ", pageListeners="
        + pageListeners + "]";
  }

}
