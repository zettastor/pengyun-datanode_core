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

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.archive.page.PageAddress;
import py.datanode.configuration.DataNodeConfiguration;
import py.datanode.exception.TimedOutWaitingForAvailablePageException;
import py.datanode.page.IoType;
import py.datanode.page.Page;
import py.datanode.page.PageContext;
import py.datanode.page.PageIoListener;
import py.datanode.page.PageManager;
import py.datanode.page.PageSelectionAlgorithm;
import py.datanode.page.PageStatus;
import py.datanode.page.PageSystemFunction;
import py.datanode.page.TaskType;
import py.datanode.page.context.BogusPageContext;
import py.datanode.page.context.PageContextFactory;
import py.datanode.page.context.PageContextWrapper;
import py.datanode.page.context.SinglePageContextFactory;
import py.datanode.storage.scheduler.StorageIoWorkerImplCollection;
import py.function.Callback;

public class PageManagerImpl implements PageManager<Page>, PageIoListener {
  private static final Logger logger = LoggerFactory.getLogger(PageManagerImpl.class);
  private static final int DEFAULT_INTERVAL_WHEN_IDLE_MS = 1000;
  public static long ZERO_DATA_CHECKSUM;
  public static byte[] ZERO_BUFFER;

  private final LinkedListMultimap<PageAddress, PageContext<Page>> contextsWaitingForFreePages;

  private final BlockingQueue<PageContext<Page>> queue;
  private final PageContextFactory<Page> pageContextFactory = SinglePageContextFactory
      .getInstance();
  private final DataNodeConfiguration cfg;

  private final DoublyLinkedHashMap<PageAddress, Page> swappingPagePool;

  private final Map<PageAddress, Page> dirtyPagePool;

  private final DoublyLinkedHashMap<PageAddress, Page> readCachePagePool;

  private final Map<PageAddress, Page> checkedOutPages;

  private final Multimap<PageAddress, PageContext<Page>> contextsWaitForLoad;

  private final StorageIoDispatcher dispatcher;
  private final boolean dispatcherCreatedByMyself;

  private final PageSelectionAlgorithm<Page> pageSelectionAlgorithm;

  private final AtomicBoolean closing = new AtomicBoolean(false);

  private final AtomicInteger flushErrors = new AtomicInteger(0);

  private final Map<PageAddress, Page> checkedOutDirtyPages;
  private final long totalPageCount;
  private final long freePageCount;
  private final PageSystemFunction[] serviceFunctions;
  private final LinkedList<PageContext<Page>> contextsToProcess;
  private final AbstractCheckin[] checkinFunctions;

  private Thread puller;

  private Multimap<PageAddress, PageContext<Page>> contextsWaitForCheckin;

  private Multimap<PageAddress, PageContext<Page>> contextsWaitForFlush;

  private String tag;

  public PageManagerImpl(List<Page> pages,
      DataNodeConfiguration cfg) {
    this(pages, cfg, "L1", null);
  }

  public PageManagerImpl(List<Page> pages,
      DataNodeConfiguration cfg,
      String tag, StorageIoDispatcher dispatcher) {
    this.contextsWaitingForFreePages = LinkedListMultimap.create();
    this.queue = new LinkedBlockingQueue<>();
    this.contextsToProcess = new LinkedList<>();
    this.contextsWaitForLoad = LinkedListMultimap.create();
    this.contextsWaitForCheckin = LinkedListMultimap.create();
    this.contextsWaitForFlush = LinkedListMultimap.create();

    this.readCachePagePool = new DoublyLinkedHashMap<>();
    this.dirtyPagePool = new HashMap<>();
    this.swappingPagePool = new DoublyLinkedHashMap<>();
    this.checkedOutPages = new HashMap<>();
    this.checkedOutDirtyPages = new HashMap<>();

    this.pageSelectionAlgorithm = new PageSelectionLruAlgorithm<Page>();
    this.cfg = cfg;
    this.tag = tag;

    initZeroBuffer(cfg.getPageSize());

    if (dispatcher == null) {
      this.dispatcher = new StorageIoDispatcher();
      this.dispatcherCreatedByMyself = true;
    } else {
      this.dispatcher = dispatcher;
      this.dispatcherCreatedByMyself = false;
    }

    totalPageCount = pages.size();
    long readPageCount = totalPageCount * cfg.getReadCachePageRatio() / 100;
    freePageCount = totalPageCount - readPageCount;

    Validate.isTrue(readPageCount >= 0);
    Validate.isTrue(freePageCount > 0);
    long tmpFreePageCount = freePageCount;
    for (Page page : pages) {
      Validate.isTrue(!page.isDirty());

      PageAddress pageAddress = page.getAddress();
      if (tmpFreePageCount > 0) {
        swappingPagePool.putFirst(pageAddress, page);
        tmpFreePageCount--;
      } else {
        readCachePagePool.putFirst(pageAddress, page);
        page.setCachedForRead(true);
        readPageCount--;
      }
    }

    long dirtyPageCount = 0;
    logger.warn(
        "dirty page count: {}, total pages: {}, swapping pages: {}, "
            + "read cache page: {}, in {} pool",
        dirtyPageCount, pages.size(), swappingPagePool.size(), readCachePagePool.size(), tag);
    serviceFunctions = new PageSystemFunction[TaskType.values().length];
    checkinFunctions = new AbstractCheckin[TaskType.values().length];
    initFunction();
  }

  public static void initZeroBuffer(int pageSize) {
    ZERO_BUFFER = new byte[pageSize];
  }

  public void start() {
    puller = new ContextPuller();
    if (dispatcherCreatedByMyself) {
      StorageIoWorkerImplCollection storageIoWorkerImplCollection =
          new StorageIoWorkerImplCollection(
              cfg);
      dispatcher.start(this, storageIoWorkerImplCollection, storageIoWorkerImplCollection,
          storageIoWorkerImplCollection);
    }
  }

  private void wakeupTimedoutTasks() {
    if (contextsWaitingForFreePages.size() == 0) {
      return;
    }

    List<PageContext<Page>> contextsToDelete = null;
    for (PageContext<Page> context : contextsWaitingForFreePages.values()) {
      if (context.isExpired()) {
        logger.warn("timeout, context: {}", context);
        context.setCause(new TimedOutWaitingForAvailablePageException());
        context.done();
        if (contextsToDelete == null) {
          contextsToDelete = new ArrayList<PageContext<Page>>();
        }
        contextsToDelete.add(context);
      } else {
        break;
      }
    }
    if (contextsToDelete != null) {
      for (PageContext<Page> context : contextsToDelete) {
        contextsWaitingForFreePages.remove(context.getPageAddressForIo(), context);
      }
    }
  }

  private void process(PageContext<Page> context) {
    try {
      serviceFunctions[context.getTaskType().getValue()].process(context);
    } catch (Exception e) {
      logger.error("caught an exception, current task: {}", context, e);
      if (context.getTaskType().isPageCheckIn()) {
        return;
      } else {
        logger.debug("taskType: {}", context.getTaskType());
        context.setCause(e);
        context.done();
      }
    } finally {
      logger.info("nothing need to do here");
    }
  }

  private void triggerPendingContext() {
    List<PageContext<Page>> contextsToDelete = null;

    for (PageContext<Page> context : contextsWaitingForFreePages.values()) {
      if (context.isExpired()) {
        context.setCause(new TimedOutWaitingForAvailablePageException());
        context.done();
        if (contextsToDelete == null) {
          contextsToDelete = new ArrayList<PageContext<Page>>();
        }
        contextsToDelete.add(context);
        logger.info("timeout context: {}", context);
      } else {
        List<PageContext<Page>> contexts = contextsWaitingForFreePages
            .removeAll(context.getPageAddressForIo());
        logger
            .info("now, someone free page, so we should get a new pending contexts: {}, total: {}",
                contexts,
                contexts.size());
        for (int i = contexts.size() - 1; i >= 0; i--) {
          contextsToProcess.addFirst(contexts.get(i));
        }
        break;
      }
    }

    if (contextsToDelete != null) {
      for (PageContext<Page> context : contextsToDelete) {
        contextsWaitingForFreePages.remove(context.getPageAddressForIo(), context);
      }
    }
  }

  @Override
  public void close() throws InterruptedException {
    boolean closedAlready = closing.getAndSet(true);
    if (closedAlready) {
      logger.warn(
          "{} page manager is being shut down by another thead. "
              + "Please wait. stack trace of this and previous shutdowns:",
          tag, new Exception());
      return;
    }
    logger.warn("shutting down {} page manager. stack trace of this and previous shutdowns:", tag,
        new Exception());

    logger.warn(
        "page manager flushed begin, total pages: {}, dirty pages count: {}, "
            + "free pages count: {}, read cache pages: {}, checkout pages: {}",
        totalPageCount, dirtyPagePool.size(), swappingPagePool.size(), readCachePagePool.size(),
        checkedOutPages.size());

    cfg.setStartFlusher(true);
    int i = 0;
    while (true) {
      int currentPageCount = swappingPagePool.size() + readCachePagePool.size();
      if (currentPageCount == totalPageCount && checkedOutPages.size() == 0) {
        break;
      }

      Thread.sleep(cfg.getInteverlForCheckPageReleasedMs());
      if ((i++) % 10 == 0) {
        logger.warn(
            "swapping pages: {}, read cache pages: {}, dirty pages: {}, "
                + "checkout pages: {}, dirty page manager: {}",
            swappingPagePool.size(), readCachePagePool.size(), dirtyPagePool,
            checkedOutPages.keySet());
      }
    }

    logger.warn(
        "page manager flushed end, dirty pages count: {}, free pages count: {},"
            + " read cache pages: {}, checkout pages: {}",
        dirtyPagePool.size(), swappingPagePool.size(), readCachePagePool.size(),
        checkedOutPages.size());

    try {
      queue.offer(BogusPageContext.defaultBogusPageContext);
      puller.join();
    } catch (Exception e) {
      logger.error("{} page manager has stop the work thread", tag, e);
    }

    if (dispatcherCreatedByMyself) {
      try {
        dispatcher.stop();
      } catch (Exception e) {
        logger.error("caught an exception when shutdown io dispatcher", e);
      }
    }

    logger.info("successfully shutdown {} page manager", tag);
  }

  @Override
  public boolean isClosed() {
    return closing.get();
  }

  private PageContext<Page> checkout(PageAddress address, TaskType taskType) {
    PageContext<Page> context = pageContextFactory
        .generateCheckoutContext(address, taskType, cfg.getDefaultPageRequestTimeoutMs());
    boolean success = queue.offer(context);
    logger.debug("offer {}, task type: {}, to queue", address, taskType);
    if (!success) {
      Validate.isTrue(false, "get address: " + address + ", for: " + taskType);
    }

    if (cfg.isEnablePageAddressValidation()) {
      if (GarbagePageAddress.isGarbagePageAddress(address) || BogusPageAddress
          .isAddressBogus(address)) {
        logger.error("invalid page address !! {}", address, new Exception());
      }
    }

    try {
      context.waitFor();
    } catch (Exception e) {
      logger.error("caught an exception", e);
    }
    return context;
  }

  @Override
  public void checkout(PageContext<Page> pageContext) {
    if (cfg.isEnablePageAddressValidation()) {
      try {
        if (pageContext instanceof PageContextWrapper) {
          PageContextWrapper<Page> pageContextWrapper = (PageContextWrapper<Page>) pageContext;
          for (PageContext context : pageContextWrapper.getPageContexts()) {
            PageAddress address = context.getPageAddressForIo();
            if (GarbagePageAddress.isGarbagePageAddress(address) || BogusPageAddress
                .isAddressBogus(address)) {
              logger.error("invalid page address !! {}", address, new Exception());
            }
          }
        } else {
          PageAddress address = pageContext.getPageAddressForIo();
          if (GarbagePageAddress.isGarbagePageAddress(address) || BogusPageAddress
              .isAddressBogus(address)) {
            logger.error("invalid page address !! {}", address, new Exception());
          }
        }
      } catch (Throwable t) {
        logger.warn("throwable caught validating page address", t);
      }
    }
    boolean success = queue.offer(pageContext);
    if (!success) {
      logger.warn("not success");
      Validate.isTrue(false, "get pageContext: " + pageContext);
    }
  }

  @Override
  public PageContext<Page> checkoutForRead(PageAddress address) {
    return checkout(address, TaskType.CHECK_OUT_FOR_READ);
  }

  @Override
  public PageContext<Page> checkoutForExternalRead(PageAddress address) {
    return checkout(address, TaskType.CHECK_OUT_FOR_EXTERNAL_READ);
  }

  @Override
  public PageContext<Page> checkoutForInternalWrite(PageAddress address) {
    return checkout(address, TaskType.CHECK_OUT_FOR_INTERNAL_WRITE);
  }

  @Override
  public PageContext<Page> checkoutForExternalWrite(PageAddress address) {
    return checkout(address, TaskType.CHECK_OUT_FOR_EXTERNAL_WRITE);
  }

  @Override
  public PageContext<Page> checkoutForInternalCorrection(PageAddress address) {
    return checkout(address, TaskType.CHECK_OUT_FOR_INTERNAL_CORRECTION);
  }

  @Override
  public PageContext<Page> checkoutForExternalCorrection(PageAddress address) {
    return checkout(address, TaskType.CHECK_OUT_FOR_EXTERNAL_CORRECTION);
  }

  @Override
  public void checkin(PageContext<Page> context) {
    if (context == null || context.getPage() == null) {
      logger.warn("can not check in the page, maybe you don't load the page for context: {}",
          context);
      return;
    }

    checkinFunctions[context.getTaskType().getValue()].checkin(context);
  }

  @Override
  public void flushPage(PageContext<Page> context) {
    boolean success = queue.offer(context);
    logger.debug("flush page, context: {}", context);
    if (!success) {
      Validate.isTrue(false, "can not submit flush pages context: " + context);
    }
  }

  public boolean isLoading(PageAddress addressToLoad, PageContext<Page> context) {
    Collection<PageContext<Page>> waitLoadingContexts = contextsWaitForLoad.get(addressToLoad);
    if (waitLoadingContexts.size() == 0) {
      return false;
    }

    if (!contextsWaitForLoad.put(addressToLoad, context)) {
      Validate.isTrue(false, "task: " + context);
    }
    return true;
  }

  private boolean movePageFromReadCachePoolToSwappingPagePool() {
    Page page = readCachePagePool.removeLastValue();
    if (page == null) {
      return false;
    }

    Validate.isTrue(page.isCachedForRead());
    page.setCachedForRead(false);
    PageAddress pageAddress = page.getAddress();

    if (pageAddress instanceof BogusPageAddress) {
      Validate.isTrue(swappingPagePool.putLast(pageAddress, page) == null);
      logger.debug("move page: {} from read cache pool to tail of swapping page pool", page);
      triggerPendingContext();
      return true;
    }

    Validate.isTrue(swappingPagePool.putFirst(pageAddress, page) == null);
    logger.debug("all level two pages have been check out, just move page: {} to swapping pool",
        page);
    triggerPendingContext();
    return true;

  }

  private void checkoutPage(PageContext<Page> context, Page page, boolean loaded) {
    Validate.isTrue(checkedOutPages.put(context.getPageAddressForIo(), page) == null);
    page.checkout();
    page.setClean(false);
    page.setPageLoaded(loaded);
    context.setPage(page);
    context.done();
  }

  @Override
  public void loadedFromStorage(PageContext<Page> context) {
    logger.debug("page has been loaded from storage, cause: {}, page: {}", context.getCause(),
        context.getPage());

    PageContext<Page> originalContext = context.getOriginalPageContext();
    Validate.isTrue(context.getIoType() == IoType.FROMDISK
        || context.getIoType() == IoType.FROMDISKFOREXTERNAL);

    context.setTaskType(TaskType.LOADED_FROM_DISK);
    if (context.isSuccess()) {
      Page page = originalContext.getPage();
      page.checkout();
      page.setPageLoaded(true);
      offerToMyself(context);

      originalContext.done();
    } else {
      offerToMyself(context);
    }
  }

  @Override
  public void flushedToStorage(PageContext<Page> context) {
    logger.debug("page has been flushed to storage, cause: {}, page: {}, io type is {}",
        context.getCause(),
        context.getPage(), context.getIoType());
    Validate.isTrue(
        context.getIoType() == IoType.TODISK || context.getIoType() == IoType.TODISKFOREXTERNAL);
    context.setTaskType(TaskType.FLUSHED_TO_DISK);
    context.done();

    offerToMyself(context);
  }

  private void offerToMyself(PageContext<Page> context) {
    if (!queue.offer(context)) {
      Validate.isTrue(false, "can not submit context when flushing over: " + context);
    }
  }

  private void offerToIoDispatcher(PageContext<Page> context) {
    logger.debug("offer an context {}", context);
    dispatcher.submit(context);
  }

  @Override
  public int recordFlushError(PageAddress pageAddress) {
    return flushErrors.getAndIncrement();
  }

  public synchronized boolean cleanCache() {
    try {
      logger.warn("clean all free pages in L1");
      PageContext<Page> context = pageContextFactory.generateCleanCacheContext();
      boolean success = queue.offer(context);
      logger.debug("offer to queue {}", context);
      if (!success) {
        Validate.isTrue(false, "can not submit check page for flush context: " + context);
      }
      context.waitFor();
      return true;
    } catch (Exception e) {
      logger.error("caught an exception when cleaning cache", e);
      return false;
    }
  }

  @Override
  public long getTotalPageCount() {
    return totalPageCount;
  }

  public long getFreePageCount() {
    return freePageCount;
  }

  public long getDirtyPageCount() {
    return dirtyPagePool.size();
  }

  @Override
  public DataNodeConfiguration getCfg() {
    return this.cfg;
  }

  private boolean wakeupContexts(PageContext<Page> context,
      Multimap<PageAddress, PageContext<Page>> multiMapwaitingContexts) {
    PageAddress address = context.getPageAddressForIo();
    List<PageContext<Page>> waitContexts = (List<PageContext<Page>>) multiMapwaitingContexts
        .removeAll(address);

    int count = 0;
    for (int i = waitContexts.size() - 1; i >= 0; i--) {
      PageContext<Page> tmpContext = waitContexts.get(i);
      if (tmpContext instanceof BogusPageContext) {
        continue;
      }

      if (multiMapwaitingContexts == contextsWaitForFlush) {
        if (tmpContext.getTaskType() == TaskType.FLUSH_PAGE) {
          logger.debug("this is a flush request, just done for page: {}", address);
          tmpContext.setCause(context.getCause());
          tmpContext.done();
          continue;
        }
      }

      contextsToProcess.addFirst(tmpContext);
      count++;
    }

    logger.debug("there are {} contexts are waiting for page: {}", count, address);
    return count > 0;
  }

  private void clearPage(Page page) {
    PageAddress newAddress = new BogusPageAddress();
    page.setPageStatus(PageStatus.FREE);
    page.changeAddress(newAddress);
    page.setCanbeFlushed();
    page.setPageLoaded(false);
    page.setClean(false);
    Validate.isTrue(swappingPagePool.putLast(newAddress, page) == null);
  }

  @Override
  public StorageIoDispatcher getStorageIoDispatcher() {
    return dispatcher;
  }

  @Override
  public void flushedToL2Write(PageContext<Page> context) {
    throw new NotImplementedException("not implement context " + context);
  }

  public void dump() {
    logger.error("@@ swappingPagePool: {}", swappingPagePool.size());
    logger.error("@@ readCachePagePool: {}", readCachePagePool.size());
    logger.error("@@ dirtyPagePool: {},{}", dirtyPagePool.size());
    logger.error("@@ checkedOutPages: {},{}", checkedOutPages.size());
    logger.error("@@ contextsBeingInvolvedIO: {},{}", contextsWaitForLoad.size());
    logger.error("@@ contextWaitingForFreePages: {},{}", contextsWaitingForFreePages.size());
  }

  private void initFunction() {
    serviceFunctions[TaskType.CHECK_OUT_FOR_READ.getValue()] = new CheckoutForRead();
    checkinFunctions[TaskType.CHECK_OUT_FOR_READ.getValue()] = new CheckinForRead();

    serviceFunctions[TaskType.CHECK_OUT_FOR_INTERNAL_WRITE.getValue()] = new CheckoutForWrite();
    checkinFunctions[TaskType.CHECK_OUT_FOR_INTERNAL_WRITE.getValue()] = new CheckinForWrite();

    serviceFunctions[TaskType.CHECK_OUT_FOR_INTERNAL_CORRECTION
        .getValue()] = new CheckoutForCorrection();
    checkinFunctions[TaskType.CHECK_OUT_FOR_INTERNAL_CORRECTION
        .getValue()] = new CheckinForCorrection();

    serviceFunctions[TaskType.CHECK_OUT_FOR_EXTERNAL_READ
        .getValue()] = new CheckoutForExternalRead();
    checkinFunctions[TaskType.CHECK_OUT_FOR_EXTERNAL_READ
        .getValue()] = new CheckinForExternalRead();

    serviceFunctions[TaskType.CHECK_OUT_FOR_EXTERNAL_WRITE.getValue()] = new CheckoutForWrite();
    checkinFunctions[TaskType.CHECK_OUT_FOR_EXTERNAL_WRITE
        .getValue()] = new CheckinForExternalWrite();

    serviceFunctions[TaskType.CHECK_OUT_FOR_EXTERNAL_CORRECTION
        .getValue()] = new CheckoutForCorrection();
    checkinFunctions[TaskType.CHECK_OUT_FOR_EXTERNAL_CORRECTION
        .getValue()] = new CheckinForExternalCorrection();

    serviceFunctions[TaskType.CHECK_IN_FOR_READ
        .getValue()] = checkinFunctions[TaskType.CHECK_OUT_FOR_READ
        .getValue()];
    serviceFunctions[TaskType.CHECK_IN_FOR_INTERNAL_WRITE
        .getValue()] = checkinFunctions[TaskType.CHECK_OUT_FOR_INTERNAL_WRITE.getValue()];
    serviceFunctions[TaskType.CHECK_IN_FOR_INTERNAL_CORRECTION
        .getValue()] = checkinFunctions[TaskType.CHECK_OUT_FOR_INTERNAL_CORRECTION.getValue()];
    serviceFunctions[TaskType.CHECK_IN_FOR_EXTERNAL_READ
        .getValue()] = checkinFunctions[TaskType.CHECK_OUT_FOR_EXTERNAL_READ.getValue()];
    serviceFunctions[TaskType.CHECK_IN_FOR_EXTERNAL_WRITE
        .getValue()] = checkinFunctions[TaskType.CHECK_OUT_FOR_EXTERNAL_WRITE.getValue()];
    serviceFunctions[TaskType.CHECK_IN_FOR_EXTERNAL_CORRECTION
        .getValue()] = checkinFunctions[TaskType.CHECK_OUT_FOR_EXTERNAL_CORRECTION.getValue()];

    serviceFunctions[TaskType.FLUSHED_TO_DISK.getValue()] = new FlushedToStorage();
    serviceFunctions[TaskType.LOADED_FROM_DISK.getValue()] = new LoadedFromStorage();
    serviceFunctions[TaskType.CLEAN_CACHE.getValue()] = new CleanCache();
    serviceFunctions[TaskType.FLUSH_PAGE.getValue()] = new FlushPage();

  }

  public abstract class AbstractCheckout implements PageSystemFunction {
    public boolean doIfHitInCheckoutPagePool(PageAddress addressToLoad, PageContext<Page> context) {
      Page page = checkedOutPages.get(addressToLoad);
      if (page == null) {
        return false;
      }

      hitInCheckoutPagePool(page, addressToLoad, context);
      return true;
    }

    public abstract void hitInCheckoutPagePool(Page page, PageAddress addressToLoad,
        PageContext<Page> context);

    public boolean doIfHitInFreePagePool(PageAddress addressToLoad, PageContext<Page> context) {
      Page page = readCachePagePool.remove(addressToLoad);
      if (page != null) {
        Validate.isTrue(page.isCachedForRead());
        hitInReadCachePagePool(page, context);
        return true;
      }

      page = swappingPagePool.remove(addressToLoad);
      if (page != null) {
        Validate.isTrue(!page.isCachedForRead());
        hitInSwappingPagePool(page, context);
        return true;
      }

      page = dirtyPagePool.remove(addressToLoad);
      if (page != null) {
        hitInDirtyPagePool(addressToLoad, page, context);
        return true;
      }
      return false;
    }

    public abstract void hitInDirtyPagePool(PageAddress addressToLoad, Page page,
        PageContext<Page> context);

    public abstract void hitInReadCachePagePool(Page page, PageContext<Page> context);

    public abstract void hitInSwappingPagePool(Page page, PageContext<Page> context);

    public void doIfMissInL1(PageContext<Page> context) {
      PageAddress addressToLoad = context.getPageAddressForIo();

      Page page = pageSelectionAlgorithm.select(swappingPagePool);
      if (page == null) {
        logger.info("there is no free page, current total pending task: {}, current context: {}",
            contextsWaitingForFreePages.size(), context);
        Validate.isTrue(contextsWaitingForFreePages.put(addressToLoad, context));

        return;
      }
      page.setClean(false);
      logger.debug("select a page from swapping page pool, page: {}", page);
      Validate.isTrue(!page.isCachedForRead());
      Validate.isTrue(!page.isDirty());
      page.setPageLoaded(false);
      PageAddress addressToBeSwappedOut = page.getAddress();
      Validate.isTrue(swappingPagePool.get(addressToBeSwappedOut) == null);
      Validate.isTrue(checkedOutPages.get(addressToBeSwappedOut) == null);

      loadDataToPage(addressToLoad, page, context);
    }

    public void loadDataToPage(PageAddress addressToLoad, Page page, PageContext<Page> context) {
      contextsWaitForLoad.put(addressToLoad, BogusPageContext.defaultBogusPageContext);
      Validate.isTrue(checkedOutPages.put(addressToLoad, page) == null);
      context.setPage(page);
      if (context.getTaskType().isCachedForRead()) {
        offerToIoDispatcher(
            pageContextFactory.generatePageLoadContext(IoType.FROMDISKFOREXTERNAL, context));
      } else {
        offerToIoDispatcher(pageContextFactory.generatePageLoadContext(IoType.FROMDISK, context));
      }
      logger.debug("loadDataToPage: {}", context);
    }
  }

  public class CheckoutForRead extends AbstractCheckout {
    @Override
    public void process(PageContext<Page> context) {
      logger.debug("check out for read, context: {}", context);
      checkout(context);
    }

    public void checkout(PageContext<Page> context) {
      PageAddress addressToLoad = context.getPageAddressForIo();
      if (doIfHitInCheckoutPagePool(addressToLoad, context)) {
        logger.debug("{}doIfHitInCheckoutPagePool return true", addressToLoad);
        return;
      }

      if (doIfHitInFreePagePool(addressToLoad, context)) {
        logger.debug("{}doIfHitInFreePagePool return true", addressToLoad);
        return;
      }

      doIfMissInL1(context);
    }

    @Override
    public void hitInSwappingPagePool(Page page, PageContext<Page> context) {
      Validate.isTrue(page.canRead());
      checkoutPage(context, page, true);
    }

    @Override
    public void hitInReadCachePagePool(Page page, PageContext<Page> context) {
      Validate.isTrue(page.canRead());
      checkoutPage(context, page, true);
    }

    @Override
    public void hitInDirtyPagePool(PageAddress addressToLoad, Page page,
        PageContext<Page> context) {
      Validate.isTrue(checkedOutDirtyPages.put(addressToLoad, page) == null);
      Validate.isTrue(page.canRead());
      checkoutPage(context, page, true);
    }

    @Override
    public void hitInCheckoutPagePool(Page page, PageAddress addressToLoad,
        PageContext<Page> context) {
      if (isLoading(addressToLoad, context)) {
        return;
      }

      if (page.canRead()) {
        page.checkout();
        Validate.isTrue(page.isPageLoaded());
        context.setPage(page);
        context.done();
        return;
      }

      if (!contextsWaitForCheckin.put(addressToLoad, context)) {
        Validate.isTrue(false, "task: " + context + ", page: " + page);
      }

      logger.debug("the page has been check out for write, can not check out for read, task: {}",
          context);
      Validate.isTrue(page.getPageStatus() != PageStatus.READ);
    }

    @Override
    public void loadDataToPage(PageAddress addressToLoad, Page page, PageContext<Page> context) {
      Validate.isTrue(page.canRead());
      super.loadDataToPage(addressToLoad, page, context);
    }

    public void hitSwapping(PageAddress addressToLoad, PageContext<Page> context,
        PageContext<Page> swapContext) {
      Page pageSwappedOut = swapContext.getPage();
      Validate.isTrue(pageSwappedOut.canRead());
      checkoutPage(context, pageSwappedOut, true);
    }
  }

  public class CheckoutForExternalRead extends CheckoutForRead {
    @Override
    public void process(PageContext<Page> context) {
      logger.debug("check out for external read, context: {}", context);
      checkout(context);
    }

    @Override
    public void hitInDirtyPagePool(PageAddress addressToLoad, Page page,
        PageContext<Page> context) {
      if (!page.isCachedForRead()) {
        if (movePageFromReadCachePoolToSwappingPagePool()) {
          page.setCachedForRead(true);
        } else {
          logger.info("read cache is full when hit in dirty page pool");
        }
      }
      super.hitInDirtyPagePool(addressToLoad, page, context);
    }

    @Override
    public void hitInCheckoutPagePool(Page page, PageAddress addressToLoad,
        PageContext<Page> context) {
      if (isLoading(addressToLoad, context)) {
        return;
      }

      if (page.canRead()) {
        if (!page.isCachedForRead()) {
          if (movePageFromReadCachePoolToSwappingPagePool()) {
            page.setCachedForRead(true);
          } else {
            logger.info("read cache is full when hit in check out pool");
          }
        }

        page.checkout();
        Validate.isTrue(page.isPageLoaded());
        context.setPage(page);
        context.done();
        return;
      }

      if (!contextsWaitForCheckin.put(addressToLoad, context)) {
        Validate.isTrue(false, "task: " + context + ", page: " + page);
      }

      logger.debug("can not get for read, task: {}, page: {}", context, page);
      Validate.isTrue(page.getPageStatus() != PageStatus.READ);
    }

    @Override
    public void hitInSwappingPagePool(Page page, PageContext<Page> context) {
      Validate.isTrue(page.canRead());

      if (movePageFromReadCachePoolToSwappingPagePool()) {
        page.setCachedForRead(true);
      }

      checkoutPage(context, page, true);
    }

    @Override
    public void hitInReadCachePagePool(Page page, PageContext<Page> context) {
      Validate.isTrue(page.canRead());

      checkoutPage(context, page, true);
    }

    @Override
    public void loadDataToPage(PageAddress addressToLoad, Page page, PageContext<Page> context) {
      super.loadDataToPage(addressToLoad, page, context);
    }

    public void hitSwapping(PageAddress addressToLoad, PageContext<Page> context,
        PageContext<Page> swapContext) {
      Page pageSwappedOut = swapContext.getPage();
      if (movePageFromReadCachePoolToSwappingPagePool()) {
        pageSwappedOut.setCachedForRead(true);
      }
      super.hitSwapping(addressToLoad, context, swapContext);
    }
  }

  public class CheckoutForWrite extends AbstractCheckout {
    @Override
    public void process(PageContext<Page> context) {
      logger.debug("check out for write, context: {}", context);
      checkout(context);
    }

    public void checkout(PageContext<Page> context) {
      PageAddress addressToLoad = context.getPageAddressForIo();
      logger.debug("checkout for write ,address is {}", addressToLoad);
      if (doIfHitInCheckoutPagePool(addressToLoad, context)) {
        logger.debug("doIfHitInCheckoutPagePool is hit");
        return;
      }

      if (doIfHitInFreePagePool(addressToLoad, context)) {
        logger.debug("doIfHitInFreePagePool is hit");
        return;
      }

      doIfMissInL1(context);
    }

    @Override
    public void hitInDirtyPagePool(PageAddress addressToLoad, Page page,
        PageContext<Page> context) {
      if (page.canWrite()) {
        logger.debug("hit in dirty page: {}", addressToLoad);

        Validate.isTrue(null == checkedOutDirtyPages.put(addressToLoad, page));
        checkoutPage(context, page, true);
        return;
      }

      Validate.isTrue(dirtyPagePool.put(addressToLoad, page) == null);

      if (!contextsWaitForFlush.put(addressToLoad, context)) {
        Validate.isTrue(false, "task: " + context + ", page: " + page);
      }

      logger.info("hold on the task: {}, page: {} until the page is flushed, don't done the task",
          context, page);
    }

    @Override
    public void hitInSwappingPagePool(Page page, PageContext<Page> context) {
      if (page.canWrite()) {
        checkoutPage(context, page, true);
        return;
      }

      Validate.isTrue(swappingPagePool.putFirst(page.getAddress(), page) == null);
      contextsToProcess.addFirst(context);
      logger.info("hit in free page: {} can is being flushed, so can not for write for context: {}",
          context,
          page);
      return;
    }

    @Override
    public void hitInReadCachePagePool(Page page, PageContext<Page> context) {
      if (page.canWrite()) {
        checkoutPage(context, page, true);
        return;
      }

      Validate.isTrue(readCachePagePool.putFirst(page.getAddress(), page) == null);
      contextsToProcess.addFirst(context);
      logger.info("hit in free page: {} can is being flushed, so can not for write for context: {}",
          context,
          page);
      return;
    }

    @Override
    public void hitInCheckoutPagePool(Page page, PageAddress addressToLoad,
        PageContext<Page> context) {
      if (isLoading(addressToLoad, context)) {
        return;
      }

      if (!contextsWaitForCheckin.put(addressToLoad, context)) {
        Validate.isTrue(false, "task: " + context + ", page: " + page);
      }

      logger.debug("can not get for write, task: {}, page: {}", page, context);
      Validate.isTrue(page.getPageStatus() != PageStatus.FREE);
      return;
    }

    @Override
    public void loadDataToPage(PageAddress addressToLoad, Page page, PageContext<Page> context) {
      if (!page.canWrite()) {
        Validate.isTrue(swappingPagePool.putFirst(page.getAddress(), page) == null);
        contextsToProcess.addFirst(context);
        logger.info("the free page: {} can is being flushed, so can not for write", page);
        return;
      }
      super.loadDataToPage(addressToLoad, page, context);
    }
  }

  public class CheckoutForCorrection extends CheckoutForWrite {
    @Override
    public void process(PageContext<Page> context) {
      logger.debug("check out for correction, context: {}", context);
      checkout(context);
    }

    @Override
    public void loadDataToPage(PageAddress addressToLoad, Page page, PageContext<Page> context) {
      if (!page.canWrite()) {
        Validate.isTrue(swappingPagePool.putFirst(page.getAddress(), page) == null);
        contextsToProcess.addFirst(context);
        logger.info("the free page: {} can is being flushed, so can not for correction", page);
        return;
      }

      Validate.isTrue(!page.isCachedForRead());
      page.changeAddress(addressToLoad);
      checkoutPage(context, page, false);
    }

  }

  public abstract class AbstractCheckin implements PageSystemFunction {
    public abstract void checkin(PageContext<Page> context);

    public void submit(PageContext<Page> context) {
      context.setTaskType(context.getTaskType().getCheckinType());
      logger.debug("checkin submit, offer the queue, {}", context);
      boolean success = queue.offer(context);
      if (!success) {
        Validate.isTrue(false, "can not submit free context: " + context);
      }
    }

    public Page freePage(PageContext<Page> context) {
      Page page = context.getPage();

      int value = page.checkin();
      if (value != 0) {
        logger.info("there are {} users using the page: {}, ioType: {}", value, page,
            context.getTaskType());
        return null;
      }

      PageAddress address = context.getPageAddressForIo();
      Page checkedOutPage = checkedOutPages.remove(address);
      if (checkedOutPage != page) {
        logger.error(
            "the page {} in page system is differenct with page: {} whecn checking in, {}, {} ",
            checkedOutPage, page, swappingPagePool.get(address), dirtyPagePool.get(address));
        throw new RuntimeException("check in page: " + address + " which is not checked out");
      }

      page.setPageStatus(PageStatus.FREE);
      return page;
    }
  }

  public class CheckinForRead extends AbstractCheckin {
    @Override
    public void checkin(PageContext<Page> context) {
      logger.debug("read use is over, just check in for page: {}", context.getPage());
      submit(context);
    }

    @Override
    public void process(PageContext<Page> context) {
      logger.debug("check in for read, context: {}", context);
      Page page = freePage(context);
      if (page == null) {
        logger.info("some other context has checkout the page: {}", page);
        return;
      }

      final boolean wakeup = wakeupContexts(context, contextsWaitForCheckin);
      PageAddress address = context.getPageAddressForIo();
      Validate.isTrue(!(address instanceof BogusPageAddress));
      if (page.isDirty()) {
        Validate.isTrue(dirtyPagePool.put(address, page) == null);
        return;
      }

      if (page.isCachedForRead()) {
        readCachePagePool.putFirst(address, page);
        return;
      }

      swappingPagePool.putFirst(address, page);

      if (!wakeup) {
        triggerPendingContext();
      }
    }
  }

  public class DirtyPageCallBack implements Callback {
    private final PageContext<Page> pageContext;

    public DirtyPageCallBack(PageContext<Page> pageContext) {
      this.pageContext = pageContextFactory.generateInnerFlushContext(pageContext.getPage());
    }

    @Override
    public void completed() {
      flushedToStorage(pageContext);
    }

    @Override
    public void failed(Throwable e) {
      pageContext.setCause((Exception) e);
      flushedToStorage(pageContext);
    }
  }

  public class CheckinForWrite extends AbstractCheckin {
    @Override
    public void checkin(PageContext<Page> context) {
      logger.debug("write use is over, just check in for page: {}", context.getPage());

      Page page = context.getPage();
      Validate.isTrue(page.isFlushing());

      PageAddress address = page.getAddress();
      if (page.isDirty()) {
        submit(context);
        page.setCanbeFlushed();
        dispatcher.getDirtyPagePoolFactory().getOrBuildDirtyPagePool(address.getStorage())
            .add(new DirtyPageCallBack(context), page, false);
      } else {
        logger.info(" {} page is not dirty for write and page has been flushed. checkout: {}",
            address,
            page.getCheckoutCount());
        submit(context);
        page.setCanbeFlushed();
      }
    }

    @Override
    public void process(PageContext<Page> context) {
      logger.debug("check in for write, context: {}", context);
      Page page = freePage(context);
      if (page == null) {
        Validate.isTrue(false,
            "write context is rejection with all other context, context: " + context);
        return;
      }

      boolean wakeup = wakeupContexts(context, contextsWaitForCheckin);

      PageAddress address = context.getPageAddressForIo();
      Validate.isTrue(!(address instanceof BogusPageAddress));

      Page dirtyPage = checkedOutDirtyPages.remove(address);
      if (dirtyPage != null) {
        Validate.isTrue(dirtyPage == page);
        Validate.isTrue(dirtyPage.isDirty());
      }

      if (page.isDirty()) {
        Validate.isTrue(dirtyPagePool.put(address, page) == null);
        return;
      }

      Validate.isTrue(dirtyPage == null);

      if (page.isCachedForRead()) {
        Validate.isTrue(readCachePagePool.putFirst(address, page) == null);
      } else {
        Validate.isTrue(swappingPagePool.putFirst(address, page) == null);
        if (!wakeup) {
          triggerPendingContext();
        }
      }
    }
  }

  public class CheckinForExternalWrite extends CheckinForWrite {
    @Override
    public void checkin(PageContext<Page> context) {
      logger.debug("write use is over, just check in for page: {}", context.getPage());

      Page page = context.getPage();
      Validate.isTrue(page.isFlushing());

      PageAddress address = page.getAddress();
      if (page.isDirty()) {
        submit(context);
        page.setCanbeFlushed();
        dispatcher.getDirtyPagePoolFactory().getOrBuildDirtyPagePool(address.getStorage())
            .add(new DirtyPageCallBack(context), page, true);
      } else {
        logger.info(" {} page is not dirty for write and page has been flushed. checkout: {}",
            address,
            page.getCheckoutCount());
        submit(context);
        page.setCanbeFlushed();
      }
    }

    @Override
    public void process(PageContext<Page> context) {
      super.process(context);
    }
  }

  public class CheckinForCorrection extends AbstractCheckin {
    @Override
    public void checkin(PageContext<Page> context) {
      Page page = context.getPage();
      logger.debug("correct use is over, success({}), just check in for context: {}",
          page.isPageLoaded(),
          context);

      if (!page.isPageLoaded()) {
        page.changeAddress(new BogusPageAddress());
        page.setDirty(false);
      }

      Validate.isTrue(page.isFlushing());
      PageAddress address = page.getAddress();
      if (page.isDirty()) {
        submit(context);
        page.setCanbeFlushed();
        dispatcher.getDirtyPagePoolFactory().getOrBuildDirtyPagePool(address.getStorage())
            .add(new DirtyPageCallBack(context), page, false);
      } else {
        logger.info(" {} page is not dirty and page has been flushed. checkout: {}", address,
            page.getCheckoutCount());
        submit(context);
        page.setCanbeFlushed();
      }
    }

    @Override
    public void process(PageContext<Page> context) {
      logger.debug("check in for correction, context: {}", context);
      Page page = freePage(context);
      if (page == null) {
        Validate.isTrue(false,
            "correction context is rejection with all other context, context: " + context);
        return;
      }

      boolean wakeup = wakeupContexts(context, contextsWaitForCheckin);
      PageAddress addressForIo = context.getPageAddressForIo();
      Page dirtyPage = checkedOutDirtyPages.remove(addressForIo);

      if (dirtyPage != null) {
        Validate.isTrue(dirtyPage == page);
      }

      PageAddress address = page.getAddress();
      if (address instanceof BogusPageAddress) {
        logger.info("can not correct page for context: {} ", context);

        swappingPagePool.putLast(address, page);
        if (!wakeup) {
          triggerPendingContext();
        }
        return;
      }

      Validate.isTrue(address.equals(addressForIo));
      if (page.isDirty()) {
        Validate.isTrue(dirtyPagePool.put(address, page) == null);
        return;
      }

      if (page.isCachedForRead()) {
        Validate.isTrue(readCachePagePool.putFirst(address, page) == null);
      } else {
        Validate.isTrue(swappingPagePool.putFirst(address, page) == null);
        if (!wakeup) {
          triggerPendingContext();
        }
      }

    }
  }

  public class CheckinForExternalCorrection extends CheckinForCorrection {
    @Override
    public void checkin(PageContext<Page> context) {
      Page page = context.getPage();
      logger.debug("correct use is over, success({}), just check in for context: {}",
          page.isPageLoaded(),
          context);

      if (!page.isPageLoaded()) {
        page.changeAddress(new BogusPageAddress());
        page.setDirty(false);
      }

      Validate.isTrue(page.isFlushing());
      PageAddress address = page.getAddress();
      if (page.isDirty()) {
        submit(context);
        page.setCanbeFlushed();
        dispatcher.getDirtyPagePoolFactory().getOrBuildDirtyPagePool(address.getStorage())
            .add(new DirtyPageCallBack(context), page, true);
      } else {
        logger.info(" {} page is not dirty and page has been flushed. checkout: {}", address,
            page.getCheckoutCount());
        submit(context);
        page.setCanbeFlushed();
      }
    }

    @Override
    public void process(PageContext<Page> context) {
      super.process(context);
    }
  }

  public class CheckinForExternalRead extends CheckinForRead {
    @Override
    public void process(PageContext<Page> context) {
      super.process(context);
    }
  }

  public class FlushedToStorage implements PageSystemFunction {
    @Override
    public void process(PageContext<Page> context) {
      logger.debug("page has been flushed to storage for context: {}", context);
      Validate.isTrue(
          context.getIoType() == IoType.TODISK || context.getIoType() == IoType.TODISKFOREXTERNAL);
      Page page = context.getPage();
      PageAddress pageAddress = context.getPageAddressForIo();

      boolean wakeup = wakeupContexts(context, contextsWaitForFlush);

      if (!context.isSuccess()) {
        logger.warn("can not flush page to storage, context: {}", context);
        flushErrors.incrementAndGet();
      }

      if (!page.isDirty()) {
        logger.warn("someone has flushed the page to storage, context: {}", context);
        page.setCanbeFlushed();
        return;
      }

      Page dirtyPage = dirtyPagePool.remove(pageAddress);
      if (dirtyPage == null) {
        logger.info("someone has been checked out for READ");
        dirtyPage = checkedOutDirtyPages.get(pageAddress);
        if (dirtyPage == null) {
          logger.warn("can not find the page: {}, {}, {}, {}", context,
              readCachePagePool.get(pageAddress),
              swappingPagePool.get(pageAddress), checkedOutPages.get(pageAddress));
          Validate.isTrue(false);
        }
        Validate.isTrue(page.getPageStatus() == PageStatus.READ);
        Validate.isTrue(checkedOutPages.get(pageAddress) == dirtyPage);
        Validate.isTrue(dirtyPage.isDirty());
        dirtyPage.setDirty(false);

        page.setCanbeFlushed();
        return;
      }

      Validate.isTrue(dirtyPage.isDirty());
      dirtyPage.setDirty(false);

      page.setCanbeFlushed();

      if (dirtyPage.isCachedForRead()) {
        readCachePagePool.putFirst(pageAddress, dirtyPage);
      } else {
        swappingPagePool.putFirst(pageAddress, dirtyPage);
        if (!wakeup) {
          triggerPendingContext();
        }
      }

    }

  }

  public class LoadedFromStorage implements PageSystemFunction {
    @Override
    public void process(PageContext<Page> context) {
      logger.debug("page has been loaded from storage for context: {}", context);
      Validate.isTrue(
          context.getIoType() == IoType.FROMDISK
              || context.getIoType() == IoType.FROMDISKFOREXTERNAL);
      PageAddress pageAddress = context.getPageAddressForIo();
      PageContext<Page> originalContext = context.getOriginalPageContext();
      Page page = context.getPage();
      Validate.isTrue(!page.isCachedForRead());
      if (!context.isSuccess()) {
        logger.warn("can not load the page for context: {}", context);

        originalContext.setCause(context.getCause());

        Validate.isTrue(checkedOutPages.remove(pageAddress) == page);

        originalContext.setPage(null);
        originalContext.done();

        Validate.isTrue(!page.isDirty());
        clearPage(page);
      } else {
        if (originalContext.getTaskType().isCachedForRead()) {
          if (movePageFromReadCachePoolToSwappingPagePool()) {
            page.setCachedForRead(true);
          }
        }
      }

      if (context.isSuccess()) {
        wakeupContexts(context, contextsWaitForLoad);
      } else {
        List<PageContext<Page>> waitForLoadingContexts =
            (List<PageContext<Page>>) contextsWaitForLoad
                .removeAll(pageAddress);
        for (int i = waitForLoadingContexts.size() - 1; i >= 0; i--) {
          PageContext<Page> waitContext = waitForLoadingContexts.get(i);
          waitContext.setCause(context.getCause());
          waitContext.done();
        }

        triggerPendingContext();
      }
    }
  }

  public class FlushPage implements PageSystemFunction {
    @Override
    public void process(PageContext<Page> context) {
      PageAddress pageAddressToFlush = context.getPageAddressForIo();
      Page page = dirtyPagePool.get(pageAddressToFlush);
      if (page != null) {
        Validate.isTrue(page.isDirty());

        logger.debug("flush the page: {} to storage ", page);

        contextsWaitForFlush.put(pageAddressToFlush, context);
        PageContext<Page> newContext = pageContextFactory.generateExternalFlushContext(page);
        dispatcher.submit(newContext);
        return;
      }

      page = checkedOutPages.get(pageAddressToFlush);
      if (page != null) {
        if (page.canFlush()) {
          logger.debug("flush the checkout page: {} to storage, can flush", page);
          if (page.isDirty()) {
            contextsWaitForFlush.put(pageAddressToFlush, context);
            PageContext<Page> newContext = pageContextFactory.generateInnerFlushContext(page);
            dispatcher.submit(newContext);
            return;
          } else {
            page.setCanbeFlushed();
          }
        } else {
          logger.debug("flush the checkout page: {} to storage, wait for flushing", page);
          if (page.isDirty()) {
            contextsWaitForCheckin.put(pageAddressToFlush, context);
            return;
          }
        }
      }

      context.done();
    }
  }

  public class CleanCache implements PageSystemFunction {
    @Override
    public void process(PageContext<Page> task) {
      long numPages = swappingPagePool.size();

      logger.warn("start, clean swapping page counts: {}", numPages);
      while (numPages > 0) {
        Page page = swappingPagePool.removeLastValue();
        BogusPageAddress bogusPageAddress = new BogusPageAddress();
        page.changeAddress(bogusPageAddress);
        Validate.isTrue(!page.isDirty());
        swappingPagePool.putFirst(bogusPageAddress, page);
        numPages--;
      }

      numPages = readCachePagePool.size();
      long startTime = System.currentTimeMillis();
      logger.warn("start, clean read cache page counts: {}, cost time: {}s", numPages,
          (System.currentTimeMillis() - startTime) / 1000);
      while (numPages > 0) {
        Page page = readCachePagePool.removeLastValue();
        BogusPageAddress bogusPageAddress = new BogusPageAddress();
        page.changeAddress(bogusPageAddress);
        Validate.isTrue(!page.isDirty());
        readCachePagePool.putFirst(bogusPageAddress, page);
        numPages--;
      }

      logger.warn("stop, clean page counts: {}, cost time: {}s", numPages,
          (System.currentTimeMillis() - startTime) / 1000);
      task.done();
    }
  }

  private class ContextPuller extends Thread {
    public ContextPuller() {
      super("pull-" + tag + "-task");
      start();
    }

    @Override
    public void run() {
      try {
        long startTime = System.currentTimeMillis();
        while (true) {
          Validate.isTrue(contextsToProcess.isEmpty());
          if (queue.drainTo(contextsToProcess) == 0) {
            PageContext<Page> pollTask = queue
                .poll(DEFAULT_INTERVAL_WHEN_IDLE_MS, TimeUnit.MILLISECONDS);
            if (pollTask != null) {
              contextsToProcess.add(pollTask);
            }
          }

          long currentTime = System.currentTimeMillis();
          if (currentTime - startTime > cfg.getDefaultPageRequestTimeoutMs()) {
            wakeupTimedoutTasks();
            startTime = currentTime;
          }

          while (!contextsToProcess.isEmpty()) {
            PageContext<Page> context = contextsToProcess.removeFirst();
            if (context instanceof BogusPageContext) {
              logger.warn("now page manager will exit");
              return;
            }
            if (context instanceof PageContextWrapper) {
              PageContextWrapper<Page> pageContextWrapper1 = (PageContextWrapper<Page>) context;
              for (PageContext<Page> pageContext1 : pageContextWrapper1.getPageContexts()) {
                if (pageContext1 instanceof PageContextWrapper) {
                  PageContextWrapper<Page> pageContextWrapper2 =
                      (PageContextWrapper<Page>) pageContext1;
                  for (PageContext<Page> pageContext2 : pageContextWrapper2.getPageContexts()) {
                    process(pageContext2);
                  }
                } else {
                  process(pageContext1);
                }
              }

              continue;
            }

            process(context);
          }
        }
      } catch (Throwable t) {
        logger.error("caught an exception, System exit", t);
        System.exit(0);
      }
    }
  }

}
