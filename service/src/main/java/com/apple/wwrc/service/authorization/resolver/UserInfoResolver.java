package com.apple.wwrc.service.authorization.resolver;

import com.apple.wwrc.foundation.configuration.ConfigurationManager;
import com.apple.wwrc.foundation.db.util.DBUtils;
import com.apple.wwrc.foundation.framework.identify.exception.AuthorizationException;
import com.apple.wwrc.foundation.framework.util.FoundationCache;
import com.apple.wwrc.service.authorization.AuthorizationConstants;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

@SuppressWarnings({"squid:S00103", "common-java:InsufficientCommentDensity", "squid:S1166", "squid:S109"})
public class UserInfoResolver {
  private static final Logger logger = LoggerFactory.getLogger(UserInfoResolver.class);
  private static final int CACHE_MAX = 5000;
  //cache ds info by prs_id, each entry is a regional map between salesOrg and a list of permissions
  private ConcurrentMap<String, Map<String, Set<String>>> cache;
  private ConfigurationManager configManager;
  private final String query = "SELECT PRS_ID, "
    + "ID_GP_WRK as ROLE, ID_SLS_ORG as SALESORG, ID_STR_RT as STORE "
    + "FROM DS_INFO "
    + "WHERE PRS_ID=? ";

  public UserInfoResolver() {
    cache = FoundationCache.createExpiringMapBasedOnAccessTime(AuthorizationConstants.DS_CACHE_TIMEOUT_MINUTE, TimeUnit.MINUTES, CACHE_MAX);
    configManager = ConfigurationManager.getInstance();
  }

  public void tearDown() {
    cache.clear();
  }

  public Map<String, Set<String>> getDSInfo(long prsId) throws AuthorizationException {
    if (cache.get(String.valueOf(prsId)) == null) {
      Map<String, Set<String>> dsInfo = loadFromDB(prsId);
      cache.put(String.valueOf(prsId), dsInfo);
    }
    return cache.get(String.valueOf(prsId));
  }

  public boolean hasEasyPayAccess(long prsId, String storeId) throws AuthorizationException {
    //get dsInfo
    Map<String, Set<String>> dsInfo = getDSInfo(prsId);
    //get saleOrg
    String salesOrg = salesOrgFromStoreId(storeId);
    if (StringUtils.isBlank(salesOrg)) {
      throw new AuthorizationException("SalesOrg not found for storeId:" + storeId);
    }
    Set<String> storePermission = dsInfo.get(salesOrg);
    if (storePermission == null) {
      return false;//User does not has access on a given salesOrg
    }
    //Note: <rdar://problem/42136587> will return Easy Access for Store Permission Roles or StoreSupport Roles
    boolean hasAtleaseOneStorePermission = false;
    boolean hasPermissionToStoreInQuestion = false;
    boolean hasGeoPermission = false;
    for (String userRoleAssignment : storePermission) {
      //A GeoPermission is an assignment with a label 'P'
      if (userRoleAssignment.endsWith("->P")) {
        hasGeoPermission = true;
      } else { //A StorePermission is an assignment that is not labeled 'P' nor 'T'
        if (!userRoleAssignment.endsWith("->T")) {
          hasAtleaseOneStorePermission |= true;
        }
      }
      //A Direct StorePermission is an assignment that is labeled as storeId
      if (userRoleAssignment.endsWith(storeId)) {
        hasPermissionToStoreInQuestion = true;
      }
    }
    return isStoreAccessRole(hasGeoPermission, hasPermissionToStoreInQuestion, hasAtleaseOneStorePermission) ||
      isGeoAccessRole(hasGeoPermission, hasPermissionToStoreInQuestion, hasAtleaseOneStorePermission);
  }

  private boolean isStoreAccessRole(boolean hasGeoPermission, boolean hasPermissionToStoreInQuestion, boolean hasAtleaseOneStorePermission) {
    return hasGeoPermission && hasPermissionToStoreInQuestion;
  }

  private boolean isGeoAccessRole(boolean hasGeoPermission, boolean hasPermissionToStoreInQuestion, boolean hasAtleaseOneStorePermission) {
    return hasGeoPermission && !hasAtleaseOneStorePermission;
  }

  public String salesOrgFromStoreId(String storeId) {
    return configManager.getSalesOrg(storeId);
  }

  private Map<String, Set<String>> loadFromDB(long prsId) throws AuthorizationException {
    logger.debug("loading full DS Info from DB prsId=" + prsId);
    try {
      Map<String, Set<String>> storeAssignment = new HashMap<>();
      List<Map<String, ?>> resultSet = DBUtils.execute(query, prsId);
      for (Map entry : resultSet) {
        String salesOrg = (String) entry.get("SALESORG");
        if (!storeAssignment.containsKey(salesOrg)) {
          storeAssignment.put(salesOrg, new HashSet<String>());
        }
        //Add role assignment entry into storeAssignment table
        String role = (String) entry.get("ROLE");
        String store = (String) entry.get("STORE");
        String roleAssignment = String.format("%s->%s", role, store);//e.g. '3->P', '3->R028'
        storeAssignment.get(salesOrg).add(roleAssignment);
      }//end for
      logger.debug(resultSet.size() + " user-role assignments loaded for prsId=" + prsId);
      return storeAssignment;
    } catch (SQLException e) {
      throw new AuthorizationException(e.getMessage(), e);
    }
  }
}
