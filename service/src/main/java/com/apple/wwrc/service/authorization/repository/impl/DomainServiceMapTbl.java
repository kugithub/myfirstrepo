package com.apple.wwrc.service.authorization.repository.impl;

import com.apple.wwrc.foundation.configuration.updater.AbstractRepository;
import com.apple.wwrc.service.authorization.datasource.DomainServiceMapRow;
import com.apple.wwrc.service.authorization.localException.NotFound;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@SuppressWarnings({"squid:S00103", "common-java:InsufficientCommentDensity", "squid:S1166", "squid:S109"})
public class DomainServiceMapTbl extends AbstractRepository<DomainServiceMapRow> {
  private static final Logger logger = LoggerFactory.getLogger(DomainServiceMapTbl.class);
  private List<DomainServiceMapRow> list;//Identity table is small, don't bother create a Map
  private final boolean fairlock = true;
  protected volatile boolean isReady = false;
  private ReentrantReadWriteLock repositoryLock = new ReentrantReadWriteLock(fairlock);

  public boolean ready() {
    return isReady;
  }

  public DomainServiceMapTbl() {
    //Add a constructor to the class, code smell, squid:S1258
  }

  @Override
  public void updateRepo(List<DomainServiceMapRow> rows) throws Exception {
    if (notChanged(rows)) {
      isReady = true;
    } else {
      //Lock the list
      List<DomainServiceMapRow> lockedList = Collections.unmodifiableList(rows);
      refreshRepository(lockedList);
      logMapSizes();
      isReady = true;
    }
  }

  private boolean notChanged(List<DomainServiceMapRow> rows) {
    return rows.isEmpty();
  }

  private void refreshRepository(List<DomainServiceMapRow> lockedList) {
    try {
      repositoryLock.writeLock().lock();
      list = lockedList;
      logger.info("SEC_DOMAIN_SERVICE updated.");
    } finally {
      repositoryLock.writeLock().unlock();
      logger.debug("refreshRepository has released writelock.");
    }
  }

  private void logMapSizes() {
    if (logger.isDebugEnabled()) {
      logger.info("Service Domain loaded...{} rows", list.size());
    }
  }

  /**
   * @return target's Domain name or throws NotFoundException
   */
  public String getTargetDomain(String serviceNameWithVersion, boolean ignoreVersion) throws NotFound {
    if (ignoreVersion) {
      return getTargetDomainIgnoreVersion(serviceNameWithVersion);
    } else {
      return getTargetDomainAndVersionUnIgnore(serviceNameWithVersion);
    }
  }

  /** Original logic -- matching ApiName as well as version */
  private String getTargetDomainAndVersionUnIgnore(String serviceNameWithVersion) throws NotFound {
    try {
      repositoryLock.readLock().lock();
      for (DomainServiceMapRow entry : list) {
        if (entry.getServiceWithVersion().equals(serviceNameWithVersion)) {
          return entry.getDomain();
        }
      }
      throw new NotFound();
    } finally {
      repositoryLock.readLock().unlock();
      logger.debug("getTargetDomain has released readlock.");
    }
  }

  /** Per Saurabh's request -- ignore Api version, use when **it happen.*/
  private String getTargetDomainIgnoreVersion(String serviceNameWithVersion) throws NotFound {
    String matcher = striptOfVersion(serviceNameWithVersion);
    try {
      repositoryLock.readLock().lock();
      for (DomainServiceMapRow entry : list) {
        if (entry.getServiceWithVersion().startsWith(matcher)) {
          return entry.getDomain();
        }
      }
      throw new NotFound();
    } finally {
      repositoryLock.readLock().unlock();
      logger.debug("getTargetDomain has released readlock.");
    }
  }

  /**
   * Input ServiceName/vx.y, Output ServiceName
   * @param serviceNameWithVersion
   * @return
   */
  protected String striptOfVersion(String serviceNameWithVersion) {
    String[] serviceNversion = serviceNameWithVersion.split("/");
    return serviceNversion[0];
  }
}
