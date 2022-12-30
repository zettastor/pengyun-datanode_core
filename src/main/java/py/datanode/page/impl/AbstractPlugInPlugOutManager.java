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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.archive.Archive;
import py.archive.ArchiveStatus;
import py.archive.ArchiveStatusListener;
import py.archive.PluginPlugoutManager;
import py.archive.disklog.DiskErrorLogManager;
import py.exception.ArchiveIsNotCleanedException;
import py.exception.ArchiveNotFoundException;
import py.exception.ArchiveStatusException;
import py.exception.ArchiveTypeNotSupportException;
import py.exception.DiskBrokenException;
import py.exception.DiskDegradeException;
import py.exception.InvalidInputException;
import py.exception.JnotifyAddListerException;
import py.exception.StorageException;
import py.thrift.share.ArchiveIsUsingExceptionThrift;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public abstract class AbstractPlugInPlugOutManager implements PluginPlugoutManager,
    ArchiveStatusListener {
  private static final Logger logger = LoggerFactory.getLogger(AbstractPlugInPlugOutManager.class);
  protected Map<Long, Archive> mapArchiveIdToArchive = new ConcurrentHashMap<>();
  protected DiskErrorLogManager diskErrorLogManager = null;

  @Override
  public void plugin(Archive archive)
      throws ArchiveTypeNotSupportException, InvalidInputException, ArchiveIsUsingExceptionThrift,
      ArchiveStatusException, JnotifyAddListerException, ArchiveIsNotCleanedException,
      StorageException {
    logger.warn("some one plug in the archive {}", archive);
    boolean newArchiveBroken = false;
    if (diskErrorLogManager != null) {
      try {
        diskErrorLogManager.putArchive(archive);
      } catch (DiskBrokenException e) {
        logger.error(
            "L2Archive has more than the threshold of storage exceptions. " 
                + "Set it status to Broken. archive: {}",
            archive, e);
        newArchiveBroken = true;
      } catch (DiskDegradeException e) {
        logger.warn("the disk has IO exception, archive {}", archive, e);
      }
    }

    logger.warn("plug in one archive {},archive metadata is {}", archive,
        archive.getArchiveMetadata());
    Archive currentArchive = this.getArchive(archive.getArchiveMetadata().getArchiveId());
    if (currentArchive == null) {
      logger.warn("archive is not in  L2 manager, archive={}", archive);
      Archive archiveBaseWithSameSerialNum = this
          .getArchiveBySerialNumber(archive.getArchiveMetadata().getSerialNumber());
      if (archiveBaseWithSameSerialNum != null) {
        logger.warn(
            "found an old archive with serial number same as the new one, old archive {}," 
                + " new archive {}",
            archive);
        ArchiveStatus oldStatus = archiveBaseWithSameSerialNum.getArchiveMetadata().getStatus();
        Validate.isTrue(
            oldStatus == ArchiveStatus.EJECTED || oldStatus == ArchiveStatus.INPROPERLY_EJECTED);

        mapArchiveIdToArchive
            .remove(archiveBaseWithSameSerialNum.getArchiveMetadata().getArchiveId());
      }

      mapArchiveIdToArchive.put(archive.getArchiveMetadata().getArchiveId(), archive);

      ArchiveStatus newStatus = null;
      ArchiveStatus statusFromDisk = archive.getArchiveMetadata().getStatus();
      if (newArchiveBroken || statusFromDisk == ArchiveStatus.BROKEN) {
        newStatus = ArchiveStatus.BROKEN;
      } else if (!isArchiveMatchConfig(archive) || (statusFromDisk
          == ArchiveStatus.CONFIG_MISMATCH)) {
        logger.warn("the newly insert disk's configuration not match, archive {}", archive);
        newStatus = ArchiveStatus.CONFIG_MISMATCH;
      } else if (statusFromDisk == ArchiveStatus.OFFLINING
          || statusFromDisk == ArchiveStatus.OFFLINED
          || statusFromDisk == ArchiveStatus.GOOD) {
        newStatus = ArchiveStatus.GOOD;
      } else if (statusFromDisk == ArchiveStatus.DEGRADED) {
        newStatus = ArchiveStatus.DEGRADED;
      } else {
        throw new ArchiveStatusException("INPROPERLY_EJECTED and EJECTED  can not get from disk");
      }
      try {
        archive.getArchiveMetadata().getStatus().validate(newStatus);
        archive.setArchiveStatus(newStatus);
      } catch (Exception ex) {
       
        logger
            .error("catch an exception when set archive status, archive {}, new status {}", archive,
                newStatus, ex);
        return;
      }
    } else {
     
      ArchiveStatus currentStatus = currentArchive.getArchiveMetadata().getStatus();
      ArchiveStatus newStatus = null;
      if (newArchiveBroken) {
        newStatus = ArchiveStatus.BROKEN;
      } else if (currentStatus == ArchiveStatus.EJECTED
          || currentStatus == ArchiveStatus.INPROPERLY_EJECTED) {
        newStatus = ArchiveStatus.GOOD;
      } else {
        newStatus = currentStatus;
      }

      currentArchive.setStorage(archive.getStorage());
      if (currentArchive.getArchiveMetadata().getDeviceName()
          .equalsIgnoreCase(currentArchive.getArchiveMetadata().getSerialNumber())) {
        currentArchive.getArchiveMetadata()
            .setSerialNumber(archive.getArchiveMetadata().getSerialNumber());
        logger.warn("this is only use for virsh machine ,serialnumber = devname");
      }

      currentArchive.getArchiveMetadata()
          .setDeviceName(archive.getArchiveMetadata().getDeviceName());
      try {
        if (currentStatus != newStatus) {
          currentStatus.validate(newStatus);
          currentArchive.setArchiveStatus(newStatus);
        }
      } catch (Exception e) {
        logger.error("can set the archive status, archive={}, new status={}", archive, newStatus);
      }

    }

  }

  @Override
  public Archive plugout(String devName, String serialNumber)
      throws ArchiveTypeNotSupportException, ArchiveNotFoundException, InterruptedException {
    Archive plugout = getArchiveBySerialNumber(serialNumber);
    logger.warn("some one plug out the archive ,devName is {},serialNumber is {}", devName,
        serialNumber);

    if (plugout == null) {
      logger.warn("can not find the devName={}, serialNumber={}", devName, serialNumber);
      return null;
    }
    ArchiveStatus currentStatus = plugout.getArchiveMetadata().getStatus();
    ArchiveStatus newStatus = (currentStatus == ArchiveStatus.OFFLINED
        || currentStatus == ArchiveStatus.CONFIG_MISMATCH) 
        ? ArchiveStatus.EJECTED :
        ArchiveStatus.INPROPERLY_EJECTED;

    logger.warn("plug out the archive that now be use ,the  manager can not work");
    try {
      logger.warn("set archive with new status {}, archive is {}", newStatus, plugout);
      currentStatus.validate(newStatus);
      switch (newStatus) {
        case EJECTED:
          plugout.setArchiveStatus(ArchiveStatus.EJECTED);
          break;
        case INPROPERLY_EJECTED:
          plugout.setArchiveStatus(ArchiveStatus.INPROPERLY_EJECTED);
          break;
        default:
          logger.warn("plug out the archvie {}status {}is exception", plugout, currentStatus);
          Validate.isTrue(false);
      }

    } catch (Exception e) {
      logger
          .error("catch an exception when set archive {} to new status {}", plugout, newStatus, e);
    }
    return plugout;
  }

  @Override
  public void hasPlugoutFinished(Archive archive) throws ArchiveIsNotCleanedException {
    logger.error("AbstractPlugInPlugOutManager is not Implemented");
    throw new NotImplementedException();
  }

  @Override
  public List<Archive> getArchives() {
    logger.error("AbstractPlugInPlugOutManager is not Implemented");
    throw new NotImplementedException();
  }

  @Override
  public Archive getArchive(long archiveId) {
    return mapArchiveIdToArchive.get(archiveId);
  }

  public Archive getArchiveBySerialNumber(String serialNum) {
    for (Archive archive : mapArchiveIdToArchive.values()) {
      if (archive.getArchiveMetadata().getSerialNumber().equalsIgnoreCase(serialNum)) {
        return archive;
      }
    }
    return null;
  }

  protected boolean isArchiveMatchConfig(Archive archive) {
    logger.warn(" the archive is match config = {}", archive);
    return true;
  }

}
