package com.apple.wwrc.service.authorization;

import com.apple.wwrc.foundation.configuration.Constants;
import com.apple.wwrc.foundation.db.manager.ConnectionManager;
import com.apple.wwrc.foundation.framework.Framework;
import com.apple.wwrc.foundation.framework.annotation.OnHealthCheck;
import com.apple.wwrc.foundation.framework.annotation.OnServiceStart;
import com.apple.wwrc.foundation.framework.annotation.OnServiceStop;
import com.apple.wwrc.foundation.framework.exception.FrameworkException;
import com.apple.wwrc.foundation.framework.identify.AuthorizationServiceIF;
import com.apple.wwrc.foundation.framework.identify.exception.AuthenticationException;
import com.apple.wwrc.foundation.framework.identify.exception.AuthorizationException;
import com.apple.wwrc.foundation.framework.service.ServiceFactory;
import com.apple.wwrc.foundation.framework.util.PlatformConstants;
import com.apple.wwrc.foundation.security.util.JWTUtils;
import com.apple.wwrc.service.authorization.loader.UserAccessPolicyLoader;
import com.apple.wwrc.service.authorization.repository.impl.UserAccessPolicyRepositoryImpl;
import com.apple.wwrc.service.authorization.resolver.ServiceResolver;
import com.apple.wwrc.service.authorization.resolver.UserInfoResolver;
import com.apple.wwrc.service.authorization.scheduler.UserAccessPolicyPullingScheduler;
import org.apache.commons.lang.StringUtils;
import org.quartz.Scheduler;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class is an APIFacade class which maintain its components needed to full fill the Authorization request.
 * @author npoolsappasit, Vimal
 */
@SuppressWarnings({"squid:S00103", "common-java:InsufficientCommentDensity", "squid:S1166", "squid:S109", "squid:S1162", "squid:S00112"})
public class AuthorizationService extends ServiceFactory implements AuthorizationServiceIF {

  private static final String NOT_AUTHORIZED = "Not Authorized";
  //Use Framework for logging Facade.
  private static Logger logger = Framework.getLogger(AuthorizationService.class);
  private UserAccessPolicyRepositoryImpl userAccessRepository;
  private UserInfoResolver easypayResolver;
  private ServiceResolver serviceResolver;
  private Scheduler userPolicyPuller;
  private Scheduler servicePolicyPuller;
  private static final int READ = 0;
  private static final int WRITE = 1;
  private static final int EXECUTE = 2;
  private static final int PULLING_INTERVAL = 5;
  private volatile boolean ready = false;

  public AuthorizationService() {
    //Null constructor, code smell, squid:S1258
  }

  @OnServiceStart
  public void onStart() throws Exception {
    logger.info("Starting {}...", Framework.getServiceInfo(AuthorizationService.class));
    Framework.getConfigurationManager();
    try {
      //initialize User base ACL
      this.userAccessRepository = ServiceFactoryProxy.getUserAccessPolicyRepository();
      this.userPolicyPuller = new UserAccessPolicyPullingScheduler()
        .setRepository(userAccessRepository)
        .setLoader(new UserAccessPolicyLoader())
        .addNamespace(Constants.ALL_NAMESPACES)
        .setPollingIntervalMinute(PULLING_INTERVAL)
        .build();
      this.userPolicyPuller.start();
      //initialize Service-2-Service ACL
      easypayResolver = new UserInfoResolver();
      serviceResolver = new ServiceResolver()
        .setDelayStartSecond(1)
        .setPullingIntervalMinute(PULLING_INTERVAL)
        .build();
      await();
      failIfNoJWTKeyLoaded();
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      onStop();
      throw e;
    }
    ready = true;
    logger.info("{} started...", Framework.getServiceInfo(AuthorizationService.class));
  }

  private void failIfNoJWTKeyLoaded() throws Exception {
    try {
      String key = ServiceFactoryProxy.sslFactory().getJWTKey();
      if (StringUtils.isBlank(key)) {
        logger.error("JWT Encryption key is null or error. Check ssl.properties");
        throw new IllegalStateException("JWT Encryption key is null or error. Check ssl.properties");
      }
    } catch (Exception e) {
      logger.error("Fail to start Authentication Server err=" + e.getMessage(), e);
      throw e;
    }
  }

  /** Wait until repository is ready */
  private void await() throws InterruptedException {
    while (!userAccessRepository.ready() ||
      !serviceResolver.ready()) {
      Thread.sleep(1000);
    }
  }

  /** JUnit only: used by JUnit to wait until Authorization service is loaded. **/
  protected boolean ready() {
    return ready;
  }

  @OnServiceStop
  public void onStop() {
    //stop DB sync scheduler
    //stop/clean-up permission repository
    ready = false;
    silentSchedulerShutdown(this.userPolicyPuller);
    silentSchedulerShutdown(this.servicePolicyPuller);
    easypayResolver.tearDown();
    serviceResolver.tearDown();
    userAccessRepository.tearDown();
    ConnectionManager.resetPool();
    logger.info("{} stopped...", Framework.getServiceInfo(AuthorizationService.class));
  }

  private void silentSchedulerShutdown(Scheduler sch) {
    try {
      sch.shutdown();
    } catch (Exception e) {
      logger.warn("ignore {} during shutdown", e.getClass().getSimpleName());
    }
  }

  @Override
  public void validatePermission(String jwt, String permissionKey) throws AuthenticationException, AuthorizationException {
    String perm = getStorePermission(jwt, "ALL", "ALL", permissionKey);
    if (perm.equals("---")) {
      logger.info("getPermission('{}', salesOrg=ALL, store=ALL) Not Authorized", permissionKey);
      throw new AuthorizationException(NOT_AUTHORIZED);
    }
  }

  @Override
  public void validateSalesOrgPermission(String jwt, String saleOrg, String permissionKey) throws AuthenticationException, AuthorizationException {
    String perm = getStorePermission(jwt, saleOrg, "ALL", permissionKey);
    if (perm.equals("---")) {
      String msg = String.format("getPermission('%s', salesOrg=%s, store=ALL) Not Authorized", permissionKey, saleOrg);
      logger.info(msg);
      throw new AuthorizationException(NOT_AUTHORIZED);
    }
  }

  @Override
  public void validateStorePermission(String jwt, String storeId, String permissionKey) throws AuthenticationException, AuthorizationException {
    String salesOrg = Framework.getConfigurationManager().getSalesOrg(storeId);
    if (StringUtils.isBlank(salesOrg)) {
      logger.info("Can't find salesOrg matching storeId={}", storeId);
      throw new AuthorizationException(NOT_AUTHORIZED);
    }
    String perm = getStorePermission(jwt, salesOrg, storeId, permissionKey);
    if (perm.equals("---")) {
      String msg = String.format("getPermission('%s', salesOrg=%s, store=%s) Not Authorized", permissionKey, salesOrg, storeId);
      logger.info(msg);
      throw new AuthorizationException(NOT_AUTHORIZED);
    }
  }

  protected String getStorePermission(String jwt, String optSalesOrg, String optStore,
    String permissionKey) throws AuthorizationException {
    Map<String, Object> userInfo;
    try {
      userInfo = JWTUtils.readToken(jwt);
    } catch (FrameworkException e) {
      logger.warn("Can't read JWT Token {}...", jwt.substring(0, 100));
      throw new AuthorizationException(e.getMessage(), e);
    }

    if (!userInfo.containsKey(PlatformConstants.CONTEXT_USER_ID)) {
      throw new AuthorizationException("No " + PlatformConstants.CONTEXT_USER_ID + "in JWT payload " + jwt.substring(0, 100) + "...");
    }

    String dsId = (String) userInfo.get(PlatformConstants.CONTEXT_USER_ID);
    int prsId;
    try {
      prsId = Integer.valueOf(dsId);
    } catch (NumberFormatException e) {
      throw new AuthorizationException("Can't cast " + dsId + " to number.");
    }
    Map<String, Set<String>> dsInfoGroupBySalesOrg = easypayResolver.getDSInfo(prsId);
    List<String> permissions = listPermissions(dsInfoGroupBySalesOrg, optSalesOrg, optStore, permissionKey);
    String permission = merge(permissions);
    String msg = String.format("getPermission(jwt=%s, %s, salesOrg=%s) %s", jwt.substring(0, 100), permissionKey, optSalesOrg, permission);
    logger.info(msg);
    return permission;
  }

  /**
   * Return a collection of permission strings 'rwx' given all roles user is currently associated.
   */
  private List<String> listPermissions(Map<String, Set<String>> dsInfoGroupBySalesOrg, String optSalesOrg,
    String optStore, String permissionKey) {
    List<String> permissions = new ArrayList<>();
    for (Map.Entry<String, Set<String>> entry : dsInfoGroupBySalesOrg.entrySet()) {
      //each Set's entry is a string in the format role->storeID (e.g. 3->R028, 3->P, 3->T, etc.)
      String userSalesORg = entry.getKey();
      if (notRelevantSalesOrg(userSalesORg, optSalesOrg)) {
        continue;
      }
      for (String storeAssignment : entry.getValue()) {
        String[] roleToStore = storeAssignment.split("->");
        String userRole = roleToStore[0];
        String userStore = roleToStore[1];
        if (notRelevanceStore(optStore, userStore)) {
          continue;
        }
        tryGetPermissionString(permissionKey, permissions, userSalesORg, userRole, userStore);
      }//end for each store assignment
    }//end for each salesOrg assignment
    return permissions;
  }

  private void tryGetPermissionString(String permissionKey, List<String> permissions, String userSalesORg,
    String userRole, String userStore) {
    try {
      String permission = userAccessRepository.getPermissionString(userRole, userSalesORg, userStore, permissionKey);
      if (permission != null) {
        String message = String.format("[roleId:%s,salesOrg:%s] = %s", userRole, userSalesORg, permission);
        logger.debug(message);
        permissions.add(permission);
      }
    } catch (AuthorizationException e) {
      logger.warn(e.getMessage(), e);
    }
  }

  /**
   * Return true if user role assignment does not contains salesOrg or a subdivision of salesOrg in question.
   * @return
   */
  private boolean notRelevantSalesOrg(String userSalesORg, String salesOrgInQuestion) {
    boolean hasSalesOrg = !StringUtils.isBlank(salesOrgInQuestion) && !salesOrgInQuestion.equalsIgnoreCase("ALL");
    boolean related = true;
    if (hasSalesOrg) {
      related = userSalesORg.equalsIgnoreCase(salesOrgInQuestion);
    }
    return !related;
  }

  /**
   * Return true if user role assignment does not assigned to storeId in question.
   * @return
   */
  private boolean notRelevanceStore(String storeInQuestion, String userStore) {
    boolean hasStoreId = !StringUtils.isBlank(storeInQuestion) && !storeInQuestion.equalsIgnoreCase("ALL");
    boolean related = true;
    if (hasStoreId) {
      related = userStore.equalsIgnoreCase(storeInQuestion);
    }
    return !related;
  }

  /**
   * Merge permissions in the same way as operation OR does. <br>
   * e.g. --x + r-- = r-x <br>
   */
  protected String merge(List<String> permissions) {
    char[] permission = new char[] {'-', '-', '-'};
    for (String perm : permissions) {
      permission[READ] = perm.charAt(READ) == 'r' ? 'r' : permission[READ];
      permission[WRITE] = perm.charAt(WRITE) == 'w' ? 'w' : permission[WRITE];
      permission[EXECUTE] = perm.charAt(EXECUTE) == 'x' ? 'x' : permission[EXECUTE];
    }
    return new String(permission);
  }

  @Override
  public List<String> getPermissions(String clientEndPointUID, String serviceEndPointUID) throws AuthorizationException {
    throw new AuthorizationException("getPermissions(" + clientEndPointUID + ", " + serviceEndPointUID + ")...Deprecated. Hints: use isJwtAuthorized or isUidAuthorized instead");
  }

  @Override
  public boolean hasEasyPayAccess(long prsId, String storeId) {
    try {
      return easypayResolver.hasEasyPayAccess(prsId, storeId);
    } catch (AuthorizationException e) {
      logger.warn("Error: " + e.getMessage() + ". Return false", e);
      return false;
    }
  }

  @Override
  public boolean isJwtAuthorized(String jwtToken, String serviceNameVersion, String apiName) throws AuthorizationException {
    String msg = "isJwtAuthorized() called with JwtToken: " + jwtToken + " ServiceNameVersion : " + serviceNameVersion + " apiName : " + apiName;
    logger.info(msg);
    if (StringUtils.isBlank(jwtToken) || StringUtils.isBlank(serviceNameVersion) || StringUtils.isBlank(apiName)) {
      logger.error("Either JWT token or ServiceName or API is empty");
      throw new AuthorizationException("Invalid Arguments for Authorization call");
    }
    String appId;
    try {
      Map<String, Object> payload = JWTUtils.readToken(jwtToken);
      appId = (String) payload.get(PlatformConstants.APP_ID);
      if (StringUtils.isBlank(appId)) {
        logger.error("APP ID extracted from JwtToken is Empty");
        throw new AuthorizationException("APP ID missing in given JWT");
      }
    } catch (Exception e) {
      logger.error("Error in extracting JWT token ! " + e.getMessage(), e);
      logger.error("Invalid JWT token  {}", jwtToken);
      throw new AuthorizationException("JWT extraction Failed");
    }
    return serviceResolver.isAppIdAuthorized(appId, serviceNameVersion, apiName);
  }

  @Override
  public boolean isUidAuthorized(String uid, String serviceNameVersion, String apiName) throws AuthorizationException {
    String msg = "isUidAuthorized() called with Uid: " + uid + " ServiceNameVersion : "
      + serviceNameVersion + " apiName : " + apiName;
    logger.info(msg);
    if (StringUtils.isBlank(uid) || StringUtils.isBlank(serviceNameVersion) || StringUtils.isBlank(apiName)) {
      logger.error("Either Uid  or ServiceName or API is empty");
      throw new AuthorizationException("Invalid Arguments for Authorization call");
    }
    return serviceResolver.isUidAuthorized(uid, serviceNameVersion, apiName);
  }

  @OnHealthCheck
  public boolean healthCheck() {
    return this.ready();
  }

}
