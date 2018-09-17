package com.apple.wwrc.service.customer;

import com.apple.wwrc.foundation.configuration.impl.POSConfiguration;
import com.apple.wwrc.foundation.framework.Framework;
import com.apple.wwrc.foundation.framework.annotation.OnHealthCheck;
import com.apple.wwrc.foundation.framework.annotation.OnServiceStart;
import com.apple.wwrc.foundation.framework.annotation.OnServiceStop;
import com.apple.wwrc.foundation.framework.exception.FrameworkException;
import com.apple.wwrc.foundation.framework.service.ServiceFactory;
import com.apple.wwrc.service.customer.exception.InvalidInputException;
import com.apple.wwrc.service.customer.model.response.DSUser;
import com.apple.wwrc.service.customer.model.response.EmployeeSearch;
import com.apple.wwrc.service.customer.resolver.AbstractUserLookup;
import com.apple.wwrc.service.customer.resolver.DirectoryServiceResolver;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import java.util.regex.Pattern;


/**
 * CustomerService: new Home for POS-Customer.
 */
@SuppressWarnings({"squid:S00103","common-java:InsufficientCommentDensity","squid:S1166","squid:S109", "squid:S00112"})
public class CustomerService extends ServiceFactory implements CustomerServiceInterface {
    private static final String DISPLAY_FORMAT = "      {} : {}";
    private static final String FORMATTER_10_CHAR_WINDOW = "%-10s";
    private static final Long SLEEP_TIME_IN_MS = 1000L;

    //Use Framework for logging Facade.
    private static final Logger LOGGER = Framework.getLogger(CustomerService.class);
    private static final Pattern VALID_EMAIL_REGEX = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);
    private AbstractUserLookup customerLookup = null;
    private POSConfiguration configurator = null;
    private volatile boolean healthy = false;

    @OnServiceStart
    public void onStart() {
        healthy = false;
        configurator = Framework.getConfigurationManager().getConfigurations("POS");
        setEnvAwait();
        customerLookup = createLookupResolver();
        healthy = true;
        LOGGER.info("Service {} started...", Framework.getServiceInfo(CustomerService.class));
    }

    private void setEnvAwait() {
        String appId = null;
        String i3Env = null;
        String ssoEnv = null;
        while (StringUtils.isBlank(appId)) {
            LOGGER.info("pulling startup configurations...");
            try {
                Thread.sleep(SLEEP_TIME_IN_MS);
                appId = configurator.getString(Constants.POS_APP_ID);
                i3Env = configurator.getString(Constants.I3_ENV);
                ssoEnv = configurator.getString(Constants.SSO_ENV);
            } catch (InterruptedException e) {
                LOGGER.info("Sleep interrupted. Shutdown the server.");
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
        System.setProperty(Constants.I3_ENV, i3Env);
        System.setProperty(Constants.SSO_ENV, ssoEnv);
        LOGGER.info("Launching Environments:");
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(DISPLAY_FORMAT, String.format(FORMATTER_10_CHAR_WINDOW, "I3_ENV"), i3Env);
            LOGGER.info(DISPLAY_FORMAT, String.format(FORMATTER_10_CHAR_WINDOW, "SSO_ENV"), ssoEnv);
            LOGGER.info(DISPLAY_FORMAT, String.format(FORMATTER_10_CHAR_WINDOW, "AppId"), appId);
        }
    }

    //For JUnit Test
    public DirectoryServiceResolver createLookupResolver() {
        return new DirectoryServiceResolver();
    }

    @OnServiceStop
    public void onStop() {
        healthy = false;
        customerLookup.tearDown();
        LOGGER.info("Service {} stopped...", Framework.getServiceInfo(CustomerService.class));
    }

    @OnHealthCheck
    public boolean onHealthCheck() {
        return healthy;
    }

    @Override
    public EmployeeSearch employeeLookup(String query, int page, int limit) throws FrameworkException {
        try {
            validateEmployeeLookupInput(query, page, limit);
        } catch (InvalidInputException invalid) {
            LOGGER.warn(String.format("employeeLookup(q=XXX, p=%d, l=%d) - %s", page, limit, invalid.getMessage()));
            //Treat Invalid as Not Found
            return customerLookup.makeSearchResponse(page, limit, null);
        }
        query = decryptPII(query);
        try {
            //can be null
            DSUser employee;
            if (isBadgeLookup(query)) {
                validateBadgeLookup(query);
                employee = customerLookup.lookupFromBadge(query, page, limit);
            } else {
                validateEmailLookup(query);
                employee = customerLookup.lookupFromEmail(query, page, limit);
            }
            //Response
            LOGGER.info("employeeLookup(q=XXX, p={}, l={}) - OK", page, limit);

            return customerLookup.makeSearchResponse(page, limit, employee);
        } catch (InvalidInputException ie) {
            LOGGER.warn("employeeLookup(q=XXX, p={}, l={}) - Invalid", page, limit);
            //Treat Invalid as Not Found
            return customerLookup.makeSearchResponse(page, limit, null);
        } catch (Exception e) {
            LOGGER.error("employeeLookup(q=XXX, p={}, l={}) - Err", page, limit, e);
            throw new FrameworkException("Server Error: See " + CustomerService.class.getSimpleName() + "'s log");
        }
    }

    protected boolean isBadgeLookup(String query) {
        return StringUtils.isNumeric(query);
    }

    protected String decryptPII(String ciferText) {
        return ciferText;
    }

    private void validateBadgeLookup(String query) throws FrameworkException {
        if (query.length() < 4 || !StringUtils.isNumeric(query)) {
            throw new InvalidInputException("Invalid Badge ID");
        }
    }

    protected void validateEmailLookup(String query) throws FrameworkException {
        if (!VALID_EMAIL_REGEX.matcher(query).find()) {
            throw new InvalidInputException("Invalid Query String");
        }
    }

    protected void validateEmployeeLookupInput(String query, int page, int limit) throws FrameworkException {
        if (StringUtils.isBlank(query)) {
            throw new InvalidInputException("Empty Query String");
        }
        if (page < 0) {
            throw new InvalidInputException("Negative Page");
        }
        if (limit <= 0) {
            throw new InvalidInputException("Zero or Negative Page Limit");
        }
    }
}
