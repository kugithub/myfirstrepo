package com.apple.wwrc.service.configuration;

import com.apple.wwrc.foundation.framework.annotation.OnHealthCheck;
import com.apple.wwrc.service.configuration.updater.impl.ServerConfigurationUpdater;
import com.apple.wwrc.service.configuration.util.ServiceFactoryProxy;
import com.apple.wwrc.foundation.configuration.ConfigurationServiceIF;
import com.apple.wwrc.foundation.configuration.Constants;
import com.apple.wwrc.foundation.configuration.exception.NotFoundException;
import com.apple.wwrc.foundation.configuration.exception.UnChangedException;
import com.apple.wwrc.foundation.configuration.repository.AbstractConfigurationRepository;
import com.apple.wwrc.foundation.configuration.repository.ConfigurationRow;
import com.apple.wwrc.service.configuration.updater.impl.PodConfigurationLoader;
import com.apple.wwrc.foundation.db.manager.ConnectionManager;
import com.apple.wwrc.foundation.framework.Framework;
import com.apple.wwrc.foundation.framework.annotation.OnServiceStart;
import com.apple.wwrc.foundation.framework.annotation.OnServiceStop;
import com.apple.wwrc.foundation.framework.exception.FrameworkException;
import com.apple.wwrc.foundation.framework.service.ServiceFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.slf4j.Logger;


/**
 * Act as a Parameter Table's proxy. Sync with its origin DB every 1 minute,
 * Off-load DB works to its fixed number of identical containers where
 * its configuration client in (possibly thousands of) micro-service containers
 * pull configurations from using rcm/event-central.
 * Add more comments in this lines sonarpull test. comment more
 */
@SuppressWarnings({"squid:S00103","common-java:InsufficientCommentDensity","squid:S1166","squid:S109"})
public class ConfigurationService extends ServiceFactory implements ConfigurationServiceIF
{
    private static final long FIFTEEN_SECONDS = 15000l;
    //Use Framework for logging Facade.
	private static Logger logger = Framework.getLogger(ConfigurationService.class);
    private AbstractConfigurationRepository repository;
    private Scheduler scheduler;
	public ConfigurationService() {
	    //default constructor
	}
	@OnServiceStart
	public void bootstrap() {
	    logger.info("Starting {} Service...", Framework.getServiceInfo(ConfigurationService.class));    
	    try {
	        this.repository = ServiceFactoryProxy.configurationRepository();
	        this.scheduler = new ServerConfigurationUpdater()
                    .setRepository(repository)
                    .setLoader(new PodConfigurationLoader())
                    .addNamespace(Constants.ALL_NAMESPACES)
	                .setPollingIntervalMinute(1)//For you Amit: we will sync to DB every 1 minute. Happy?
	                .build();
	        this.scheduler.start();
	        await();
	        logger.info("Service {} started...", Framework.getServiceInfo(ConfigurationService.class));
	    } catch (Exception e) {
	        logger.error(e.getMessage(), e);
	        tearDown();
	    }
    }

	private void await() {
	    while (!repository.isReady()) {
	        logger.info("Await for the Configuration Server cache initialization...");
	        try {
                Thread.sleep(FIFTEEN_SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
	    }
    }

    @OnServiceStop
	public void tearDown() {
	    try {
	        if (this.scheduler != null && !this.scheduler.isShutdown()) {
	            this.scheduler.shutdown();
	        }
	        ConnectionManager.resetPool();
	    } catch (SchedulerException e) {
            logger.error(e.getMessage(), e);
        }
	    this.repository.tearDown();
	    logger.info("Service {} stopped...", Framework.getServiceInfo(ConfigurationService.class));
	}

	@Override
    public List<ConfigurationRow> getConfigurationRows(String namespace, String checksum, int begin) 
            throws FrameworkException {
	    return getConfigurationRowsPagination(namespace,checksum,begin,Constants.PAGINATION_SIZE);
    }

    @Override
    public List<ConfigurationRow> getConfigurationRowsPagination(String namespace, String checksum, int begin, int pageSize)
            throws FrameworkException {
        List<ConfigurationRow> rows;
        try {
            List<ConfigurationRow> results = repository.getConfigurationRows(namespace, checksum);
            int end = Math.min(begin+pageSize, results.size());//Avoid IndexOutOfBound
            if (begin > end) {
                logger.warn("Begin > End, return empty list");
                rows = new ArrayList<>();
            } else {
                rows = results.subList(begin, end);
            }
            rows.add(ConfigurationRow.parityRow(repository.getChecksum(namespace)));
            logger.info(formatReport(namespace, checksum, begin, "OK"));
        } catch (UnChangedException e) {
            logger.info(formatReport(namespace, checksum, begin, "UNC"));
            rows = Arrays.asList(ConfigurationRow.UNCHAGED);
        } catch (NotFoundException e) {
            String errMsg = e.getMessage();
            logger.info(formatReport(namespace, checksum, begin, "ERR\n\t-->"+errMsg.substring(0,errMsg.length()-10)));
            rows = Arrays.asList(ConfigurationRow.NOTFOUND);
        } catch (Exception e) {
            logger.info(formatReport(namespace, checksum, begin, "ERR\n\t-->"+e.toString()));
            rows = new ArrayList<>(); // not support Collection.emptyList() for RPC (yet)
        }
        return rows;
    }

    private String formatReport(String namespace, String checksum, int begin, String result) {
        return String.format("req[NS:%s, cksm:%s, bgn:%d] - %s", namespace,checksum,begin,result);
    }

    @OnHealthCheck
    public boolean healthCheck() {
        if (null == repository) {
            return false;
        } else {
            return repository.isReady();
        }
    }
}
