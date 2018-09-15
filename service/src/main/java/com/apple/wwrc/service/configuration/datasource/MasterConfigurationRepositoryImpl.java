package com.apple.wwrc.service.configuration.datasource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.apple.wwrc.foundation.configuration.Constants;
import com.apple.wwrc.foundation.configuration.exception.ConfiguratorException;
import com.apple.wwrc.foundation.configuration.exception.NotFoundException;
import com.apple.wwrc.foundation.configuration.exception.UnChangedException;
import com.apple.wwrc.foundation.configuration.impl.ConfigurationContext;
import com.apple.wwrc.foundation.configuration.impl.POSConfiguration;
import com.apple.wwrc.foundation.configuration.repository.AbstractConfigurationRepository;
import com.apple.wwrc.foundation.configuration.repository.ConfigurationRow;

/**
 * Master Configuration Repository represents the PARAMETER Table
 */
@SuppressWarnings({"squid:S00103","common-java:InsufficientCommentDensity","squid:S1166","squid:S109"})
public class MasterConfigurationRepositoryImpl extends AbstractConfigurationRepository {
    private static final int MAGIC_NUMBER = 52;
    private static final int THIRTY_PIXELS = 30;
    private static Logger logger = LogManager.getLogger(MasterConfigurationRepositoryImpl.class);
    private static volatile MasterConfigurationRepositoryImpl onlyInstance;
    private volatile Map<String, List<ConfigurationRow>> configurationMap = new HashMap<>();
    private volatile Map<String, String> checkSumMap = new HashMap<>();
    private List<ConfigurationRow> store2salesOrgList = Collections.emptyList();
    private List<ConfigurationRow> parameterList =Collections.emptyList();
    private static final boolean FAIR_LOCK = true;
    private ReentrantReadWriteLock repositoryLock = new ReentrantReadWriteLock(FAIR_LOCK);
    private volatile boolean ready = false;
    /**
     * Get singleton instance of ConfigurationRepository class
     */
    public static synchronized AbstractConfigurationRepository getInstance() {
        if (null == onlyInstance) {
            onlyInstance = new MasterConfigurationRepositoryImpl();
        }
        return onlyInstance;
    }
    @Override
    public POSConfiguration getConfigurations(ConfigurationContext userContext) {
        logger.warn("getConfigurations Not implemented in " + getClass().getSimpleName());
        return null;//NO OPERATION
    }
    @Override
    public void updateRepo(List<ConfigurationRow> configurationRows) throws Exception {
        long bgn = System.currentTimeMillis();
        if (configurationRows.isEmpty()) {
            ready = !checkSumMap.isEmpty(); //means that there are already something in its cache.
            return;
        }
        // Use local localConfigurationMap, Store2salesOrg and Parameter 
        // to keep track of the raw data. This is thread safe.
        Map<String, List<ConfigurationRow>> localConfigurationMap = new HashMap<>();
        List<ConfigurationRow> localStore2salesOrgList = new LinkedList<>();
        List<ConfigurationRow> localParameterList = new LinkedList<>();
        //Update rows
        for (ConfigurationRow configurationRow : configurationRows) {
            String nameSpace = configurationRow.getNamespace();
            if (!localConfigurationMap.containsKey(nameSpace)) {
                localConfigurationMap.put(nameSpace, new ArrayList<ConfigurationRow>());
            }
            localConfigurationMap.get(nameSpace).add(configurationRow);
            //populate the local list where an empty list indicates the raw data has not changed.
            List<ConfigurationRow> rawData = nameSpace.equals(Constants.SALES_ORG_STOREID_MAP_NAMESPACE)? 
                    localStore2salesOrgList : localParameterList;
            rawData.add(configurationRow);
        }
        //Calculate checksum
        logger.info("Calculate checksum for each namespace...");
        for (Map.Entry<String, List<ConfigurationRow>> entry : localConfigurationMap.entrySet()) {
            String namespace = entry.getKey();
            int cksum = entry.getValue().hashCode();
            checkSumMap.put(namespace, String.valueOf(cksum));
            logger.info(String.format("  + %-30s : % 15d", 
                    namespace.substring(0, Math.min(THIRTY_PIXELS,namespace.length())), cksum));
        }
        int allNamespacesCksum = configurationRows.hashCode();
        checkSumMap.put(Constants.ALL_NAMESPACES, String.valueOf(allNamespacesCksum));
        logger.info(StringUtils.repeat("=", MAGIC_NUMBER));
        logger.info(String.format("  + %-30s : % 15d", Constants.ALL_NAMESPACES, allNamespacesCksum));
        logger.info(StringUtils.repeat("=", MAGIC_NUMBER));
        //Update the cache/repo
        Map<String, List<ConfigurationRow>> lockedConfigurationMap = Collections.unmodifiableMap(localConfigurationMap);
        reloadCache(lockedConfigurationMap,localStore2salesOrgList,localParameterList);
        ready = true;
        logger.info("reload configuration finished in " + (System.currentTimeMillis() - bgn) + " msecs");
    }

    private void reloadCache(Map<String, List<ConfigurationRow>> lockedConfigurationMap, 
            List<ConfigurationRow> localStore2salesOrgList, List<ConfigurationRow> localParameterList) {
        try {
            repositoryLock.writeLock().lock();
            /*
             * MERGE THE MAP!!!
             *   There is an incident where parameter tbl has changed but store_to_salesorg not changed.
             *   If we use the map override, the store_to_salesorg map will end up empty as
             *   PodConfigurationLoader will return zero entry for store_to_salesorg, and hence,
             *   make store_to_salesorg map empty.
             *   Example of an incidence:
             *     (RPCConfigurationLoader.java:131) - namespace:salesorg-storeid-map, value:NOTFOUND
             */
            for (Map.Entry<String, List<ConfigurationRow>> nsMap : lockedConfigurationMap.entrySet()) {
                configurationMap.put(nsMap.getKey(), nsMap.getValue());
            }
            updateAllNamespaces(localStore2salesOrgList, localParameterList);
            logger.info("Configuration Cache reloaded.");
        } finally {
            repositoryLock.writeLock().unlock();
            logger.debug("Have released lock on in memory map updating block.");
        }
    }

    /**
     *  Update the all namespaces configurations:
     *      Empty localStore2salesOrgList indicates no change in PA_STR_RTL table --> use old value,
     *      Empty localParameterList indicates no change in PARAMETER table --> use old value,
     **/
    private void updateAllNamespaces(List<ConfigurationRow> localStore2salesOrgList, 
            List<ConfigurationRow> localParameterList) {
        if (!localStore2salesOrgList.isEmpty()) {
            logger.debug("PA_STR_RTL has changed, update raw data");
            store2salesOrgList = localStore2salesOrgList;
        }
        if (!localParameterList.isEmpty()) {
            logger.debug("PARAMETER has changed, update raw data");
            parameterList = localParameterList;
        }
        List<ConfigurationRow> allNamespaces = new LinkedList<>();
        allNamespaces.addAll(store2salesOrgList);
        allNamespaces.addAll(parameterList);
        configurationMap.put(Constants.ALL_NAMESPACES, allNamespaces);
    }

    @Override
    public List<ConfigurationRow> getConfigurationRows(String namespace, String checksum) 
            throws ConfiguratorException {
        List<ConfigurationRow> result = new ArrayList<>();
        if (!isReady()) {
            throw new NotFoundException("The Server is not in ready state to provide a service --> return as NotFound");
        }
        try {
            repositoryLock.readLock().lock();
            if (checkSumMap.get(namespace) == null) {
                throw new NotFoundException("namespace " + namespace + " not found. Check your DB for ID_GRP=" + namespace);
            }
            if (checkSumMap.get(namespace).equals(checksum)) {
                throw new UnChangedException(namespace);
            }
            List<ConfigurationRow> rows = configurationMap.get(namespace);
            if (rows == null) {
                throw new NotFoundException("namespace found but no row matching for namespace=" + namespace + ". Need further investigation.");
            }
            //Call System.arraycopy which should be the fastest deep cloning
            result = new ArrayList<>(rows); 
        } finally {
            repositoryLock.readLock().unlock();
        }
        return result;
    }
    @Override
    public List<ConfigurationRow> getConfigurationsForFileCache() {
        return Collections.emptyList();//Not cache anything
    }
    @Override
    public Set<String> bootstrap() {
        return new HashSet<>();
    }
    @Override
    public void tearDown() {
        ready = false;
    }
    @Override
    public String getChecksum(String namespace) {
        return checkSumMap.getOrDefault(namespace, "");
    }
    @Override
    public boolean isReady() {
        return ready;
    }
}
