package com.apple.wwrc.service.authorization.repository.impl;

import com.apple.wwrc.foundation.configuration.updater.AbstractRepository;
import com.apple.wwrc.service.authorization.datasource.SrcIdApiMapRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@SuppressWarnings({"squid:S00103", "common-java:InsufficientCommentDensity", "squid:S1166", "squid:S109"})
public class SrcIdApiMapTbl extends AbstractRepository<SrcIdApiMapRow> {
  private static final Logger logger = LoggerFactory.getLogger(SrcIdApiMapTbl.class);
  private Map<String, List<SrcIdApiMapRow>> map;//This is a Cartesian product, could be large.
  private final boolean fairlock = true;
  protected volatile boolean isReady = false;
  private ReentrantReadWriteLock repositoryLock = new ReentrantReadWriteLock(fairlock);

  public boolean ready() {
    return isReady;
  }

  public SrcIdApiMapTbl() {
    //add a constructor to a class, code smell, squid:S1258
  }

  @Override
  public void updateRepo(List<SrcIdApiMapRow> rows) throws Exception {
    if (notChanged(rows)) {
      isReady = true;
    } else {
      Map<String, List<SrcIdApiMapRow>> localMap = new HashMap<>();
      for (SrcIdApiMapRow row : rows) {
        String srcNtarget = key(row.sourceId(), striptOfVersion(row.targetService()));
        if (!localMap.containsKey(srcNtarget)) {
          localMap.put(srcNtarget, new ArrayList<SrcIdApiMapRow>());
        }
        localMap.get(srcNtarget).add(row);
      }
      //Lock the map
      Map<String, List<SrcIdApiMapRow>> lockedMap = Collections.unmodifiableMap(localMap);
      refreshRepository(lockedMap);
      logMapSizes();
      isReady = true;
    }
  }

  private String key(String src, String dst) {
    return String.format("%s-->%s", src, dst);
  }

  private boolean notChanged(List<SrcIdApiMapRow> rows) {
    return rows.isEmpty();
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

  private void refreshRepository(Map<String, List<SrcIdApiMapRow>> lockedMap) {
    try {
      repositoryLock.writeLock().lock();
      map = lockedMap;
      logger.info("SEC_SOURCEID_API_MAP updated.");
    } finally {
      repositoryLock.writeLock().unlock();
      logger.debug("refreshRepository has released writelock.");
    }
  }

  private void logMapSizes() {
    if (logger.isDebugEnabled()) {
      StringBuilder sb = new StringBuilder();
      sb.append("\n\nService-to-Service Permissions loaded:\n");
      sb.append("======================================\n");
      for (Map.Entry<String, List<SrcIdApiMapRow>> entry : map.entrySet()) {
        sb.append(String.format("    %-50s :%3d rules", entry.getKey(), entry.getValue().size()));
        sb.append("\n");
      }
      logger.info(sb.toString());
    }
  }

  /**
   * @return True if a given permission is found (in cached DB)
   */
  public boolean hasPermission(String uIdOrAppId, String serviceNameVer, String apiName, boolean ignoreVersion) {
    String serviceName = striptOfVersion(serviceNameVer);
    String searchKey = key(uIdOrAppId, serviceName);
    if (ignoreVersion) {
      try {
        repositoryLock.readLock().lock();
        return hasACLMatchingServiceIgnoreVersion(uIdOrAppId, apiName, serviceName, searchKey);
      } finally {
        repositoryLock.readLock().unlock();
        logger.debug("hasPermission has released readlock.");
      }
    } else {
      try {
        repositoryLock.readLock().lock();
        return hasACLMatchingServiceAndVersion(uIdOrAppId, serviceNameVer, apiName, searchKey);
      } finally {
        repositoryLock.readLock().unlock();
        logger.debug("hasPermission has released readlock.");
      }
    }
  }

  /** Per Saurabh's request -- ignore Api version -- use only when **it happen */
  private boolean hasACLMatchingServiceIgnoreVersion(String uIdOrAppId, String apiName, String serviceName,
    String searchKey) {
    if (!map.containsKey(searchKey)) {
      return false;
    }
    for (SrcIdApiMapRow rule : map.get(searchKey)) {
      if (rule.targetService().startsWith(serviceName) && rule.apiName().equals(apiName) && rule.sourceId().equals(uIdOrAppId)) {
        return true;
      }
    }
    return false;
  }

  /** Original logic -- exact matching ApiName as well as version */
  private boolean hasACLMatchingServiceAndVersion(String uIdOrAppId, String serviceNameVer, String apiName,
    String searchKey) {
    if (!map.containsKey(searchKey)) {
      return false;
    }
    for (SrcIdApiMapRow rule : map.get(searchKey)) {
      if (rule.targetService().equals(serviceNameVer) && rule.apiName().equals(apiName) && rule.sourceId().equals(uIdOrAppId)) {
        return true;
      }
    }
    return false;
  }
}
