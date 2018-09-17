package com.apple.wwrc.service.authorization.loader;

import com.apple.wwrc.foundation.configuration.updater.AbstractLoader;
import com.apple.wwrc.foundation.db.manager.ConnectionManager;
import com.apple.wwrc.foundation.db.util.DBUtils;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;

/**
 * Pagination load data from Database. Base class for UID and User AccessPolicy. 
 * @author npoolsappasit
 *
 * @param <T>
 */
@SuppressWarnings({"squid:S00103", "common-java:InsufficientCommentDensity", "squid:S1166", "squid:S109"})
public abstract class AbstractPolicyLoader<T> extends AbstractLoader<T> {
  protected static final Logger logger = LoggerFactory.getLogger(AbstractPolicyLoader.class);
  protected String countQuery;//to be overrided by subclass
  protected String loadQuery;//to be overrided by subclass
  protected String snapshotQuery;//to be overrided by subclass
  protected String dBSnapshot = "0000";//to be used by subclass
  private static final int PAGESIZE = 800;
  private final ThreadFactory policyLoaderThreadFactory;

  public AbstractPolicyLoader() {
    ConnectionManager.init();
    policyLoaderThreadFactory = new ThreadFactoryBuilder().setNameFormat("policy-loader-thread-%d").build();

  }

  @Override
  public List<T> load(Set<String> filters) {
    //Smart pulling: Don't pull of ORA_ROWSCN has not changed
    if (!dataSourceHasChanged()) {
      return Collections.emptyList();
    }
    long bgn = System.currentTimeMillis();
    int total;
    try {
      total = DBUtils.count(countQuery);
    } catch (SQLException e1) {
      logger.error(e1.getMessage());
      return Collections.emptyList();
    }
    int buckets = (int) Math.ceil(total / (double) PAGESIZE);

    ExecutorService pool = Executors.newFixedThreadPool(buckets, policyLoaderThreadFactory);
    List<Future<List<T>>> futures = new ArrayList<>();
    for (int i = 0; i < buckets; i++) {
      int start = i * PAGESIZE;
      Callable<List<T>> task = () -> {
        List<Map<String, ?>> resultSet = Collections.emptyList();
        List<T> rows = new ArrayList<>(PAGESIZE);
        resultSet = DBUtils.execute(loadQuery, start, PAGESIZE);
        for (Map map : resultSet) {
          T row = resultSetToRow(map);
          rows.add(row);
        }
        return rows;
      };//end-task
      futures.add(pool.submit(task));
    }//end-for
    List<T> rules = new ArrayList<>(buckets * PAGESIZE);
    for (Future<List<T>> f : futures) {
      try {
        List<T> partition = f.get();
        logger.info("Received {} result rows.", partition.size());
        rules.addAll(partition);
      } catch (Exception e) {
        logger.warn(e.getMessage());
      }
    }
    shutdownPool(pool);
    logger.info("total rows fetched....{}", rules.size());
    logger.info("pulling from DB finish in {} msec.", (System.currentTimeMillis() - bgn));
    return rules;
  }

  private void shutdownPool(ExecutorService pool) {
    pool.shutdownNow();
  }

  /**
   * Call DB Snapshot Query MAX(ORA_ROWSCN) to verify if the Table has been changed recently?
   * @return
   */
  protected boolean dataSourceHasChanged() {
    try {
      List<Map<String, ?>> resultSet = DBUtils.execute(snapshotQuery);
      String snapshot = (String) resultSet.get(0).get("SNAPSHOT");
      if (snapshot.equals(dBSnapshot)) {
        String dataSource = dataSource();
        logger.info("{} Not Changed. No DB Pulling", dataSource);
        return false;
      } else {
        dBSnapshot = snapshot;//update the snapshot
        return true;
      }
    } catch (SQLException e1) {
      logger.warn("{} need to pull from DB...", e1.getMessage());
      return true;
    }
  }

  /** return name of the DB Table datasource */
  protected abstract String dataSource();

  /** Convert resultSet to Row<T>. Please override me. */
  protected abstract T resultSetToRow(Map map);
}
