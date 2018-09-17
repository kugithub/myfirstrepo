package com.apple.wwrc.service.authorization.resolver;

import com.apple.wwrc.foundation.configuration.Constants;
import com.apple.wwrc.foundation.configuration.impl.POSConfiguration;
import com.apple.wwrc.foundation.framework.Framework;
import com.apple.wwrc.foundation.framework.identify.exception.AuthorizationException;
import com.apple.wwrc.foundation.framework.logger.LogLine;
import com.apple.wwrc.service.authorization.loader.DomainServiceMapLoaderOracleDB;
import com.apple.wwrc.service.authorization.loader.ServiceIdentityLoaderOracleDB;
import com.apple.wwrc.service.authorization.loader.SrcIdApiMapLoaderOracleDB;
import com.apple.wwrc.service.authorization.localException.NotFound;
import com.apple.wwrc.service.authorization.repository.impl.DomainServiceMapTbl;
import com.apple.wwrc.service.authorization.repository.impl.ServiceIdentityTbl;
import com.apple.wwrc.service.authorization.repository.impl.SrcIdApiMapTbl;
import com.apple.wwrc.service.authorization.scheduler.DomainServiceMapScheduler;
import com.apple.wwrc.service.authorization.scheduler.ServiceIdentityScheduler;
import com.apple.wwrc.service.authorization.scheduler.SrcIdApiMapScheduler;
import org.apache.commons.lang.StringUtils;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class have all methods for service to service Authorization requests.
 * @author Vimal
 */
@SuppressWarnings({"squid:S00103", "common-java:InsufficientCommentDensity", "squid:S1166", "squid:S109"})
public class ServiceResolver {
  private static final String TARGET_SERVICE = "TARGET_SERVICE";
  private static final String APP_ID = "APP_ID";
  private static final String NOT_FOUND_IN_CACHED_DB = " not found in cached DB";
  private static final Logger logger = LoggerFactory.getLogger(ServiceResolver.class);
  private static final String IGNORE_API_VERSION = "retailkit.authorization.ignoreVersion";
  private static final String DOMAIN_PLATFORM = "PLATFORM";
  private ServiceIdentityTbl serviceIdentityTable;
  private DomainServiceMapTbl domainServiceMapTable;
  private SrcIdApiMapTbl srcIdApiMapTbl;
  private Scheduler serviceIdentityTblPuller;
  private Scheduler domainServiceTblPuller;
  private Scheduler service2servicePermissionTblPuller;
  private int pullingIntervalMinute = 5;
  private int delayStartSeconds = 0;
  private POSConfiguration configurator;

  public ServiceResolver() {
    configurator = Framework.getConfigurationManager().getConfigurations("POS");
  }

  public ServiceResolver setPullingIntervalMinute(int interval) {
    this.pullingIntervalMinute = interval;
    return this;
  }

  public ServiceResolver setDelayStartSecond(int second) {
    this.delayStartSeconds = second;
    return this;
  }

  public ServiceResolver build() throws SchedulerException {
    //SEC_SERVICE_IDENTITY
    logger.info("    Start SEC_SERVICE_IDENTITY scheduler...");
    serviceIdentityTable = new ServiceIdentityTbl();
    serviceIdentityTblPuller = new ServiceIdentityScheduler()
      .setRepository(serviceIdentityTable)
      .setLoader(new ServiceIdentityLoaderOracleDB())
      .addNamespace(Constants.ALL_NAMESPACES)
      .setPollingIntervalMinute(pullingIntervalMinute)
      .build();
    serviceIdentityTblPuller.startDelayed(delayStartSeconds);
    //SEC_DOMAIN_SERVICE_MAP
    logger.info("    Start SEC_DOMAIN_SERVICE_MAP scheduler...");
    domainServiceMapTable = new DomainServiceMapTbl();
    domainServiceTblPuller = new DomainServiceMapScheduler()
      .setRepository(domainServiceMapTable)
      .setLoader(new DomainServiceMapLoaderOracleDB())
      .addNamespace(Constants.ALL_NAMESPACES)
      .setPollingIntervalMinute(pullingIntervalMinute)
      .build();
    domainServiceTblPuller.startDelayed(delayStartSeconds);
    //SEC_SOURCEID_API_MAP
    logger.info("    Start SEC_SOURCEID_API_MAP scheduler...");
    srcIdApiMapTbl = new SrcIdApiMapTbl();
    service2servicePermissionTblPuller = new SrcIdApiMapScheduler()
      .setRepository(srcIdApiMapTbl)
      .setLoader(new SrcIdApiMapLoaderOracleDB())
      .addNamespace(Constants.ALL_NAMESPACES)
      .setPollingIntervalMinute(pullingIntervalMinute)
      .build();
    service2servicePermissionTblPuller.startDelayed(delayStartSeconds);
    return this;
  }

  public boolean ready() {
    return serviceIdentityTable.ready() && domainServiceMapTable.ready() && srcIdApiMapTbl.ready();
  }

  /**
   * Validate whether AppId is authorized to access service
   */
  public boolean isAppIdAuthorized(String appId, String serviceNameVersion, String apiName) throws AuthorizationException {
    logger.debug(LogLine.m("Authorization with Source APP_ID Starts with ")
      .kv(APP_ID, appId)
      .kv(TARGET_SERVICE, serviceNameVersion)
      .kv("API", apiName)
      .build());
    try {
      boolean isAuthorized = validAccess(appId, serviceNameVersion, apiName);
      logger.info(LogLine.m("Authorization with APP_ID called with ")
        .kv(APP_ID, appId)
        .kv(TARGET_SERVICE, serviceNameVersion)
        .kv("API", apiName)
        .kv("isAuthorized ", isAuthorized).build());
      return isAuthorized;
    } catch (AuthorizationException err) {
      logger.error(LogLine.m("isAppIdAuthorized ERROR: " + err.getMessage())
        .kv("From Source APP ID ", appId)
        .kv("Calling Service ", serviceNameVersion)
        .kv("Api Name ", apiName).build());
      throw new AuthorizationException("Authorization ERROR. see Authentication Server's Log");
    }
  }

  /**
   * Validate whether UID is authorized to access service
   */
  public boolean isUidAuthorized(String uId, String serviceNameVersion, String apiName) throws AuthorizationException {
    logger.debug(LogLine.m("Authorization with UID Starts.").kv("UID", uId)
      .kv(TARGET_SERVICE, serviceNameVersion).kv("API", apiName).build());
    try {
      boolean isAuthorized = validAccess(uId, serviceNameVersion, apiName);
      logger.info(LogLine.m("Authorization with APP_ID called with ").kv(APP_ID, uId).kv(TARGET_SERVICE, serviceNameVersion)
        .kv("API", apiName).kv("isAuthorized ", isAuthorized).build());
      return isAuthorized;
    } catch (AuthorizationException err) {
      logger.error(LogLine.m("isUidAuthorized ERROR: " + err.getMessage())
        .kv("From Source APP ID ", uId)
        .kv("Calling Service ", serviceNameVersion)
        .kv("Api Name ", apiName).build());
      throw new AuthorizationException("Authorization ERROR. see Authentication Server's Log");
    }
  }

  public void tearDown() {
    shopSchedule(service2servicePermissionTblPuller);
    shopSchedule(domainServiceTblPuller);
    shopSchedule(serviceIdentityTblPuller);
  }

  private void shopSchedule(Scheduler scheduler) {
    try {
      if (scheduler != null && scheduler.isShutdown()) {
        scheduler.shutdown();
      }
    } catch (SchedulerException e) {
      logger.warn(e.getMessage() + " while stopping " + scheduler.getClass().getSimpleName());
    }
  }

  protected boolean validAccess(String uIdOrAppId, String serviceNameVersion, String apiName) throws AuthorizationException {
    if (StringUtils.isBlank(uIdOrAppId) || StringUtils.isBlank(serviceNameVersion) || StringUtils.isBlank(apiName)) {
      logger.error("Either sourceId or ServiceName or apiName is empty");
      throw new AuthorizationException("Invalid Arguments for Authorization call");
    }
    boolean ignoreVersion = configurator.getBoolean(IGNORE_API_VERSION, false);
    String sourceDomain = getSourceDomain(uIdOrAppId);
    String serviceDomain = getTargetDomain(serviceNameVersion, ignoreVersion);

    //RuleNo.1: Allow access to DOMAIN:PLATFORM
    if (serviceDomain.equals(DOMAIN_PLATFORM)) {
      return true;
    }
    //RuleNo.2: Same Domain
    if (sourceDomain.equals(serviceDomain)) {
      return true;
    }

    //RuleNo.3:
    return hasPermission(uIdOrAppId, serviceNameVersion, apiName, ignoreVersion);
  }

  protected String getSourceDomain(String uIdOrAppId) throws AuthorizationException {
    try {
      String nullableSrcDomain = serviceIdentityTable.getSourceDomain(uIdOrAppId);
      return nullableSrcDomain == null ? "" : nullableSrcDomain;
    } catch (NotFound e) {
      logger.error("{} not found in cached DB", uIdOrAppId);
      throw new AuthorizationException(uIdOrAppId + NOT_FOUND_IN_CACHED_DB);
    }
  }

  protected String getTargetDomain(String serviceNameVersion, boolean ignoreVersion) throws AuthorizationException {
    try {
      String serviceDomain = domainServiceMapTable.getTargetDomain(serviceNameVersion, ignoreVersion);
      if (StringUtils.isBlank(serviceDomain)) {
        logger.error("Null or Empty Domain found for serviceName={}. DBTeam... Fix it!!!", serviceNameVersion);
        throw new AuthorizationException("Null or Empty Domain found for serviceName=" + serviceNameVersion + ". @DBTeam Fix it!!!");
      } else {
        return serviceDomain;
      }
    } catch (NotFound e) {
      logger.error("{} not found in cached DB", serviceNameVersion);
      throw new AuthorizationException(serviceNameVersion + NOT_FOUND_IN_CACHED_DB);
    }
  }

  protected boolean hasPermission(String uIdOrAppId, String serviceNameVersion, String apiName, boolean ignoreVersion) {
    return srcIdApiMapTbl.hasPermission(uIdOrAppId, serviceNameVersion, apiName, ignoreVersion);
  }
}
