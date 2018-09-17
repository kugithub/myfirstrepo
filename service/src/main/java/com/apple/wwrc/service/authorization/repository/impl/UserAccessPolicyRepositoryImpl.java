package com.apple.wwrc.service.authorization.repository.impl;

import com.apple.wwrc.foundation.configuration.exception.ConfiguratorException;
import com.apple.wwrc.foundation.configuration.impl.ConfigurationContext;
import com.apple.wwrc.foundation.configuration.updater.AbstractRepository;
import com.apple.wwrc.foundation.framework.identify.exception.AuthorizationException;
import com.apple.wwrc.service.authorization.datasource.UserAccessPolicyRow;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings({"squid:S00103", "common-java:InsufficientCommentDensity", "squid:S1166", "squid:S109"})
public class UserAccessPolicyRepositoryImpl extends AbstractRepository<UserAccessPolicyRow> {
  private static final Logger logger = LoggerFactory.getLogger(UserAccessPolicyRepositoryImpl.class);
  private static final String DENIED = "---";
  private volatile AtomicInteger loadCount = new AtomicInteger(0);
  // Permission Maps
  private volatile Map<ConfigurationContext, Map<String, String>> roleMap = new HashMap<>(); //Permissions by Role
  private volatile Map<ConfigurationContext, Map<String, String>> roleSalesorgMap = new HashMap<>(); //Permission by Role-SalesOrg
  private volatile Map<ConfigurationContext, Map<String, String>> roleSalesorgStoreMap = new HashMap<>(); //Permission by Role-SalesOrg-Store
  // cache
  private Set<String> namespaces = new HashSet<>();
  private Map<ConfigurationContext, Map<String, String>> permissionCache = new HashMap<>();
  // Semaphore
  private final boolean fairlock = true;
  private ReentrantReadWriteLock repositoryLock = new ReentrantReadWriteLock(fairlock);
  private volatile boolean isReady = false;

  @Override
  public void updateRepo(List<UserAccessPolicyRow> rows) throws Exception {
    isReady = true;
    if (rows.isEmpty()) {
      return;
    }
    Map<ConfigurationContext, Map<String, String>> localRoleMap = new HashMap<>();
    Map<ConfigurationContext, Map<String, String>> localRoleSalesorgMap = new HashMap<>();
    Map<ConfigurationContext, Map<String, String>> localRoleSalesorgStoreMap = new HashMap<>();

    for (UserAccessPolicyRow row : rows) {
      String config_set_name = row.getConfigSetName();
      String role = String.valueOf(row.getRoleId());
      String saleOrg = row.getSalesOrg();
      String storeId = row.getStore();
      String key = row.getPermissionKey();
      String value = row.getValue();

      boolean hasSalesOrg = hasSalesOrg(saleOrg);
      boolean hasStore = hasSalesOrg(storeId);
      if (hasSalesOrg && hasStore) {
        ConfigurationContext configContext = ConfigurationContext._newConfigurationContext(config_set_name, role, saleOrg, storeId, "");
        appendToRawPropertiesMap(localRoleSalesorgStoreMap, configContext, key, value);
      } else if (hasSalesOrg && !hasStore) {
        ConfigurationContext configContext = ConfigurationContext._newConfigurationContext(config_set_name, role, saleOrg, "", "");
        appendToRawPropertiesMap(localRoleSalesorgMap, configContext, key, value);
      } else if (!hasSalesOrg && !hasStore) {
        ConfigurationContext configContext = ConfigurationContext._newConfigurationContext(config_set_name, role, "", "", "");
        appendToRawPropertiesMap(localRoleMap, configContext, key, value);
      } else {
        StringBuilder errorMessage = new StringBuilder()
          .append("AccessPolicyRow row is not a valid: ")
          .append(row)
          .append(". Configurations of the form [S,T], [S], and [] are supported where [S]alesOrg,S[T]ore");
        logger.warn(errorMessage.toString());
      }
      namespaces.add(role);
    }
    //Lock the maps
    Map<ConfigurationContext, Map<String, String>> lockedRoleSalesorgStoreMap = Collections.unmodifiableMap(localRoleSalesorgStoreMap);
    Map<ConfigurationContext, Map<String, String>> lockedRoleSalesorgMap = Collections.unmodifiableMap(localRoleSalesorgMap);
    Map<ConfigurationContext, Map<String, String>> lockedRoleMap = Collections.unmodifiableMap(localRoleMap);
    refreshRepository(lockedRoleSalesorgStoreMap, lockedRoleSalesorgMap, lockedRoleMap);
    flushRepositoryCache();
    //Report time
    int newLoadCount = loadCount.incrementAndGet();
    logger.info("Load count updated. LOAD_COUNT:" + newLoadCount);
    logMapSizes();
  }

  private void flushRepositoryCache() {
    logger.debug("Flush Repository Cache Data.");
    try {
      repositoryLock.writeLock().lock();
      permissionCache.clear();
      logger.debug("Successfully flush all cached permissions.");
    } finally {
      repositoryLock.writeLock().unlock();
      logger.debug("flushRepositoryCache has released writelock.");
    }
  }

  private void refreshRepository(Map<ConfigurationContext, Map<String, String>> lockedRoleSalesorgStoreMap,
    Map<ConfigurationContext, Map<String, String>> lockedRoleSalesorgMap,
    Map<ConfigurationContext, Map<String, String>> lockedRoleMap) {
    try {
      repositoryLock.writeLock().lock();
      roleSalesorgStoreMap = lockedRoleSalesorgStoreMap;
      roleSalesorgMap = lockedRoleSalesorgMap;
      roleMap = lockedRoleMap;
      logger.info("AccessPolicy Repository updated.");
    } finally {
      repositoryLock.writeLock().unlock();
      logger.debug("refreshRepository has released writelock.");
    }
  }

  /**
   * Append permission based on configuration context properties.
   * @throws ConfiguratorException
   */
  private void appendToRawPropertiesMap(Map<ConfigurationContext, Map<String, String>> permissionGroupMap,
    ConfigurationContext context, String key, String value) {
    Map<String, String> contextedAccessRules = permissionGroupMap.get(context);
    if (contextedAccessRules == null) {
      contextedAccessRules = new HashMap<>();
      permissionGroupMap.put(context, contextedAccessRules);
    }
    if (contextedAccessRules.containsKey(key)) {
      logger.warn(String.format("Override exisitng rule: CONTEXT:%s,PERM:%s,VALUE:%s", context, key, value));
    }
    contextedAccessRules.put(key.trim().toUpperCase(), value);
    logger.debug(String.format("Adding %s,%s PERM:%s,%s", context.getNamespace(), context.getSalesOrg(), key, value));
  }

  /**
   * Logs the map sizes.
   */
  private void logMapSizes() {
    if (logger.isDebugEnabled()) {
      logger.info("Role SalesOrg Store Map SIZE:" + roleSalesorgStoreMap.size());
      logger.info("      Role SalesOrg Map SIZE:" + roleSalesorgMap.size());
      logger.info("               Role Map SIZE:" + roleMap.size());
    }
  }

  /**
   * @return a permission string (e.g. r--, rw-, rwx, r-x, etc) given a <role,opt_salesOrg,opt_store> and permission key.
   */
  public String getPermissionString(String roleId, String optSaleOrg, String optStore, String permissionKey) throws AuthorizationException {
    boolean hasRole = !StringUtils.isBlank(roleId);
    boolean hasSaleOrg = hasSalesOrg(optSaleOrg);
    boolean hasStore = hasStoreId(optSaleOrg, optStore);
    //Prepare context
    ConfigurationContext context;
    if (isStorePermission(hasRole, hasSaleOrg, hasStore)) {
      context = new ConfigurationContext(roleId, optSaleOrg, optStore);
    } else if (isGeoPermission(hasRole, hasSaleOrg, hasStore)) {
      context = new ConfigurationContext(roleId, optSaleOrg);
    } else if (isGlobalPermission(hasRole, hasSaleOrg, hasStore)) {
      context = new ConfigurationContext(roleId);
    } else {
      StringBuilder errorMessage = new StringBuilder()
        .append("permission request is invalid: ")
        .append(String.format("[R=%s,S=%s,T=%s,K=%s]", roleId, optSaleOrg, optStore, permissionKey))
        .append(". Request of the form [R,S,T], [R,S], and [R] are supported where [R]ole, [S]alesOrg, and S[T]ore");
      logger.warn(errorMessage.toString());
      throw new AuthorizationException(errorMessage.toString());
    }
    //Execute policy decision
    String permission = DENIED;
    try {
      repositoryLock.readLock().lock();
      if (permissionCache.containsKey(context)) {
        permission = permissionCache.get(context).get(permissionKey.trim().toUpperCase());
      }
      Map<String, String> unmodifiableMap = generatePermissionMapForContext(context);
      permissionCache.put(context, unmodifiableMap);
      permission = unmodifiableMap.get(permissionKey.trim().toUpperCase());
    } finally {
      repositoryLock.readLock().unlock();
      logger.debug("getPermissions has released writelock.");
    }
    return StringUtils.isBlank(permission) ? DENIED : permission;
  }

  private boolean isGlobalPermission(boolean hasRole, boolean hasSaleOrg, boolean hasStore) {
    return hasRole && !hasSaleOrg && !hasStore;
  }

  private boolean isGeoPermission(boolean hasRole, boolean hasSaleOrg, boolean hasStore) {
    return hasRole && hasSaleOrg && !hasStore;
  }

  private boolean isStorePermission(boolean hasRole, boolean hasSaleOrg, boolean hasStore) {
    return hasRole && hasSaleOrg && hasStore;
  }

  private boolean hasStoreId(String optSaleOrg, String optStore) {
    return !StringUtils.isBlank(optStore) && !optSaleOrg.equalsIgnoreCase("ALL");
  }

  private boolean hasSalesOrg(String optSaleOrg) {
    return hasStoreId(optSaleOrg, optSaleOrg);
  }

  /**
   * Generate a composite key value map for configuration context.<br>
   * Use <b>readLock</b> to wait for prior repository update thread to finish --> ensure data freshness
   * @param context
   * @return the map
   */
  @SuppressWarnings("unchecked")
  private Map<String, String> generatePermissionMapForContext(ConfigurationContext context) {
    Map<String, String> compositeMap;
    if (isNotDefaultContext(context)) {
      ConfigurationContext defaultCntxt = context.cloneWithMarkedDefaultConfigSetName();
      Map<String, String> defaultMap = createCompositePermissionMap(defaultCntxt);
      Map<String, String> customMap = createCompositePermissionMap(context);
      compositeMap = merge(defaultMap, customMap);
    } else {
      compositeMap = createCompositePermissionMap(context);
    }
    return Collections.unmodifiableMap(compositeMap);
  }

  private boolean isNotDefaultContext(ConfigurationContext context) {
    return !context.getConfigSetName().equals(ConfigurationContext.DEFAULT_);
  }

  /**
   * Create a composite configuration map from be getting all relevant KeyValue pairs from ordered hierarchy ConfigurationContext.
   */
  @SuppressWarnings("unchecked")
  private Map<String, String> createCompositePermissionMap(ConfigurationContext context) {
    Map<String, String> roleSaleorgStore = getPermissionMapOrEmpty(context, roleSalesorgStoreMap);
    Map<String, String> roleSaleorg = getPermissionMapOrEmpty(context.cloneWithMaskedStoreRegister(), roleSalesorgMap);
    Map<String, String> role = getPermissionMapOrEmpty(context.cloneWithMaskedSalesorgStoreRegister(), roleMap);
    return merge(role, roleSaleorg, roleSaleorgStore);
  }

  private Map<String, String> getPermissionMapOrEmpty(ConfigurationContext context, Map<ConfigurationContext, Map<String, String>> permissionMap) {
    Map<String, String> map = permissionMap.get(context);
    return map != null ? map : new HashMap<>();
  }

  /**
   * Merge all KeyValue pairs across permission maps where most specific context override less specific context.<br>
   * @Usages: Ordered most specific map to the right
   * <b>e.g. merge</b>(roleMap, saleOrgMap, storeMap)
   */
  @SuppressWarnings("unchecked")
  private Map<String, String> merge(Map<String, String>... maps) {
    //Smart merge: Filter out empty map
    List<Map<String, String>> notEmpty = new ArrayList<>();
    for (Map<String, String> m : maps) {
      if (m != null && m.size() > 0) {
        notEmpty.add(m);
      }
    }
    if (notEmpty.isEmpty()) {
      return Collections.emptyMap();
    }
    if (notEmpty.size() == 1) {
      return notEmpty.get(0);
    }
    Map<String, String>[] mapArray = notEmpty.toArray((Map<String, String>[]) new HashMap<?, ?>[notEmpty.size()]);
    return Stream.of(mapArray)
      .flatMap(map -> map.entrySet().stream())
      .collect(Collectors.toMap(
        Map.Entry<String, String>::getKey,
        Map.Entry<String, String>::getValue,
        (v1, v2) -> v2));
  }

  public void tearDown() {
    isReady = false;
    refreshRepository(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
  }

  /** JUnit only used for smoke test **/
  protected int getLoadCount() {
    return loadCount.get();
  }

  /** JUnit only used for the JUnit thread to be blocked until Repository is ready **/
  public boolean ready() {
    return isReady;
  }
}
