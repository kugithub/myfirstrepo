package com.apple.wwrc.service.authorization.repository.impl;

import com.apple.wwrc.foundation.configuration.updater.AbstractRepository;
import com.apple.wwrc.service.authorization.datasource.ServiceIdentityRow;
import com.apple.wwrc.service.authorization.localException.NotFound;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@SuppressWarnings({"squid:S00103", "common-java:InsufficientCommentDensity", "squid:S1166", "squid:S109"})
public class ServiceIdentityTbl extends AbstractRepository<ServiceIdentityRow> {
  private static final Logger logger = LoggerFactory.getLogger(ServiceIdentityTbl.class);
  private List<ServiceIdentityRow> list;//Identity table is small, don't bother create a Map
  private final boolean fairlock = true;
  protected volatile boolean isReady = false;
  private ReentrantReadWriteLock repositoryLock = new ReentrantReadWriteLock(fairlock);

  public boolean ready() {
    return isReady;
  }

  public ServiceIdentityTbl() {
    //add a constructor to a class, code smell, squid:S1258
  }

  @Override
  public void updateRepo(List<ServiceIdentityRow> rows) throws Exception {
    if (notChanged(rows)) {
      //Skip update
      isReady = true;
    } else {
      //Lock the maps
      List<ServiceIdentityRow> lockedMap = Collections.unmodifiableList(rows);
      refreshRepository(lockedMap);
      logMapSizes();
      isReady = true;
    }
  }

  private boolean notChanged(List<ServiceIdentityRow> rows) {
    return rows.isEmpty();
  }

  private void refreshRepository(List<ServiceIdentityRow> lockedList) {
    try {
      repositoryLock.writeLock().lock();
      list = lockedList;
      logger.info("SEC_SERVICE_IDENTITIES updated.");
    } finally {
      repositoryLock.writeLock().unlock();
      logger.debug("refreshRepository has released writelock.");
    }
  }

  /**
   * Logs the map sizes.
   */
  private void logMapSizes() {
    if (logger.isDebugEnabled()) {
      logger.info("Service identities loaded..." + list.size() + " rows");
    }
  }

  /**
   * @return nullable source's domain name or throws NotFoundException
   */
  public String getSourceDomain(String sourceId) throws NotFound {
    try {
      repositoryLock.readLock().lock();
      for (ServiceIdentityRow row : list) {
        if (row.getSourceId().equals(sourceId)) {
          return row.getDomainName();
        }
      }
      throw new NotFound();
    } finally {
      repositoryLock.readLock().unlock();
      logger.debug("getTargetDomain has released readlock.");
    }
  }
}
