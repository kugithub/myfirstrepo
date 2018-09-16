package com.apple.wwrc.service.configuration.updater.impl;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;

import com.apple.wwrc.foundation.configuration.Constants;
import com.apple.wwrc.foundation.configuration.impl.ConfigurationContext;
import com.apple.wwrc.foundation.configuration.repository.ConfigurationRow;
import com.apple.wwrc.foundation.configuration.updater.AbstractLoader;
import com.apple.wwrc.foundation.db.manager.ConnectionManager;
import com.apple.wwrc.foundation.db.util.DBUtils;
import com.apple.wwrc.foundation.framework.Framework;

/**
 * Configuration Loader that load configurations from Database.
 */
@SuppressWarnings({"squid:S00103","common-java:InsufficientCommentDensity","squid:S1166","squid:S109"})
public class PodConfigurationLoader extends AbstractLoader<ConfigurationRow> {
    private static Logger logger = Framework.getLogger(PodConfigurationLoader.class);
    private static final String LOAD_ALL_CONFIGS ="SELECT CONFIG_SET_NAME, ID_GRP as NS, "
            + "ID_SLS_ORG as SALEORG, ID_STR_RT as STORE, ID_WS as REGISTER, NAME, VALUE " 
            + "FROM parameter ORDER BY CONFIG_SET_NAME,ID_GRP,ID_SLS_ORG,ID_STR_RT,ID_WS,NAME "
            + "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY"; //--> takes 320 seconds single thread and 80 seconds in multi-thread 
//    private final String query ="SELECT CONFIG_SET_NAME, ID_GRP as NS, ID_SLS_ORG as SALEORG, ID_STR_RT as STORE, ID_WS as REGISTER, NAME, VALUE " 
//            + "FROM parameter"; //--> takes 261 seconds single thread
    public static final String STORE_SALES_ORG_QUERY = "SELECT ID_STR_RT,ID_SLS_ORG FROM PA_STR_RTL";
    public static final String SELECT_STORE_ID = "SELECT store_id FROM store_list";
    public static final String SELECT_DATA_CENTER = 
            "SELECT DATA_DEFAULT FROM ALL_TAB_COLS WHERE TABLE_NAME='TR_TRN_MD' and COLUMN_NAME='DATACENTER'";
    private static final int PAGE_SIZE = 2500;
    public static final String LAST_UPDATE_QUERY_PARAM = "SELECT MAX(ORA_ROWSCN) as SNAPSHOT from PARAMETER";
    public static final String LAST_UPDATE_QUERY_STR_RTL = "SELECT MAX(ORA_ROWSCN) as SNAPSHOT from PA_STR_RTL";
    private String paramSnapshot = "0000";
    private String str_rtl_snapshot = "0000";
    private volatile boolean isloading = false;
    private final ThreadFactory configLoaderThreadFactory;
    private List<ConfigurationRow> staticConfigurations = null;
    public PodConfigurationLoader() {
        ConnectionManager.init();
        configLoaderThreadFactory = new ThreadFactoryBuilder().setNameFormat("configuraion-updater-thread-%d").build();
    }


    public List<ConfigurationRow> load(Set<String> namespaces) {
        //Quick and dirty test
        if (isloading) {
            logger.warn("Previous task still in the middle of it works. Skipped");
            return  Collections.emptyList();
        }

        //The real work begin
        try {
            isloading = true;
            long bgn = System.currentTimeMillis();
            List<ConfigurationRow> statics = loadStaticConfigurations();
            List<ConfigurationRow> storeTosalesorg = loadFromSTR_RTLTable();
            List<ConfigurationRow> parameters = loadFromParameterTable();
            logger.info("pulling from DB finish in {} msec.", (System.currentTimeMillis() - bgn));
            List<ConfigurationRow> configurations = new ArrayList<>(storeTosalesorg.size() + parameters.size());
            configurations.addAll(statics);
            configurations.addAll(storeTosalesorg);
            configurations.addAll(parameters);
            return configurations;
        } finally {
            isloading = false;
        }
    }
    /** Collect configurations which won't change on-the-fly */
    private List<ConfigurationRow> loadStaticConfigurations() {
        //Don't pull if already exist -- design to work like getLastDBSnapshot
        if (staticConfigurations != null) {
            logger.info("Already existed. No DB Pulling");
            return Collections.emptyList();
        }
        logger.info("Collecting static configurations...");
        List<ConfigurationRow> rows = new ArrayList<>();
        addDatacenterValue(rows);
        staticConfigurations = rows;
        return staticConfigurations;
    }
    /** Get datacenter ID from TR_TRN_MD table */
    private void addDatacenterValue(List<ConfigurationRow> rows) {
        try {
            List<Map<String, ?>> resultSet = DBUtils.execute(SELECT_DATA_CENTER);
            for (Map map : resultSet) {
                ConfigurationRow row = toDatacenterRow(map);
                rows.add(row);
            }
            logger.info("Done adding datacenter value");
        } catch (SQLException e1) {
            logger.warn(e1.getMessage() + " while adding datacenter value");
        }
    }

    private List<ConfigurationRow> loadFromSTR_RTLTable() {
        //Don't pull if not changed
        String snapshot = str_rtl_snapshot;
        try {
            snapshot = getLastDBSnapshot(LAST_UPDATE_QUERY_STR_RTL);
            if (snapshot.equals(str_rtl_snapshot)) {
                logger.info("PA_STR_RTL Not Changed. No DB Pulling");
                return Collections.emptyList();
            }
        } catch (SQLException e1) {
            logger.warn(e1.getMessage() + " while pulling from DB...");
        }

        //Pull from DB
        ExecutorService pool = Executors.newFixedThreadPool(1, configLoaderThreadFactory);
        List<Future<List<ConfigurationRow>>> futures = new ArrayList<>();
        //Load store-to-saleOrg mapping
        Callable<List<ConfigurationRow>> storeTosaleorgTask = () -> {
            logger.info("Loading storeId-saleOrg-map from PA_STR_RTL table...");
            List<Map<String,?>> resultSet = Collections.emptyList();
            List<ConfigurationRow> rows = new ArrayList<>(PAGE_SIZE);
            resultSet = DBUtils.execute(STORE_SALES_ORG_QUERY);
            for (Map map : resultSet) {
                ConfigurationRow row = storeSaleOrgToConfigurationRow(map);
                rows.add(row);
            }
            return rows;
        };//end-task
        futures.add(pool.submit(storeTosaleorgTask));
        List<ConfigurationRow> configurations = collect(futures);
        logger.info("total store-to-salesOrg fetched....{}", configurations.size());
        str_rtl_snapshot = snapshot;//update the snapshot at the end
        shutdownThreadPool(pool);
        return configurations;
    }


    private List<ConfigurationRow> collect(List<Future<List<ConfigurationRow>>> futures) {
        List<ConfigurationRow> configurations = new ArrayList<>();
        for(Future<List<ConfigurationRow>> f : futures) {
            try {
                List<ConfigurationRow> partition = f.get();
                logger.info("Received {} result rows.", partition.size());
                configurations.addAll(partition);
            } catch (InterruptedException e) {
                logger.warn(e.getMessage());
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                logger.warn(e.getMessage());
            }
        }
        return configurations;
    }

    private List<ConfigurationRow> loadFromParameterTable() {
        //Don't pull if not changed
        String snapshot = paramSnapshot;
        try {
            snapshot = getLastDBSnapshot(LAST_UPDATE_QUERY_PARAM);
            if (snapshot.equals(paramSnapshot)) {
                logger.info("PARAMETER Not Changed. No DB Pulling");
                return Collections.emptyList();
            }
        } catch (SQLException e1) {
            logger.warn(e1.getMessage() + " while pulling from DB...");
        }
        int total;
        try {
            total = DBUtils.count("SELECT Count(*) FROM PARAMETER");
        } catch (SQLException e1) {
            logger.error(e1.getMessage());
            return Collections.emptyList();
        }
        int buckets = (int) Math.ceil(total/(double)PAGE_SIZE);


        ExecutorService pool = Executors.newFixedThreadPool(buckets, configLoaderThreadFactory);
        List<Future<List<ConfigurationRow>>> futures = new ArrayList<>();
        for (int i=0; i<buckets; i++) {
            int start = i*PAGE_SIZE;
            Callable<List<ConfigurationRow>> task = () -> {
                List<Map<String,?>> resultSet = Collections.emptyList();
                List<ConfigurationRow> rows = new ArrayList<>(PAGE_SIZE);
                resultSet = DBUtils.execute(LOAD_ALL_CONFIGS, start, PAGE_SIZE);
                for (Map map : resultSet) {
                    ConfigurationRow row = resultSetToConfigurationRow(map);
                    rows.add(row);
                }
                return rows;
            };//end-task
            futures.add(pool.submit(task));
        }//end-for
        List<ConfigurationRow> configurations = collect(futures);
        logger.info("total configurations fetched....{}", configurations.size());
        paramSnapshot = snapshot;//update the snapshot at the end
        shutdownThreadPool(pool);
        return configurations;
    }

    private void shutdownThreadPool(ExecutorService pool) {
        pool.shutdown();
        try {
            if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private String getLastDBSnapshot(String snapshotQuery) throws SQLException {
		List<Map<String,?>> resultSet = DBUtils.execute(snapshotQuery);
		return (String) resultSet.get(0).get("SNAPSHOT");
	}

	private ConfigurationRow resultSetToConfigurationRow(Map map) {
        String configSetName = ((String) map.get("CONFIG_SET_NAME")).trim();
        String namespace = ((String) map.get("NS")).trim();
        String saleOrg = ((String) map.get("SALEORG")).trim();
        String store = ((String) map.get("STORE")).trim();
        String register = ((String) map.get("REGISTER")).trim();
        String key = ((String) map.get("NAME")).trim();
        String value = ((String) map.get("VALUE"));
        value = value == null? "" : value.trim();
        return new ConfigurationRow(configSetName, namespace, saleOrg, store, register, key, value);
    }

    private ConfigurationRow storeSaleOrgToConfigurationRow(Map map) {
        String store = ((String) map.get("ID_STR_RT")).trim();
        String saleOrg = ((String) map.get("ID_SLS_ORG")).trim();
        ConfigurationRow row = new ConfigurationRow();
        row.setConfigSetName(ConfigurationContext.DEFAULT_);
        row.setNamespace(Constants.SALES_ORG_STOREID_MAP_NAMESPACE);
        row.setSalesOrg("ALL");
        row.setStore("ALL");
        row.setRegister("ALL");
        row.setKey(store);
        row.setValue(saleOrg);
        return row;
    }

    private ConfigurationRow toDatacenterRow(Map map) {
        ConfigurationRow row = new ConfigurationRow();
        row.setConfigSetName(ConfigurationContext.DEFAULT_);
        row.setNamespace("data.center.default.map");
        row.setSalesOrg("ALL");
        row.setStore("ALL");
        row.setRegister("ALL");
        row.setKey("DATA_DEFAULT");
        row.setValue( ((String) map.get("DATA_DEFAULT")).trim() );
        return row;
    }
}
