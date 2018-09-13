package com.apple.wwrc.service.customer;

import com.apple.ist.ds2.pub.common.PersonTypeI;
import com.apple.wwrc.foundation.configuration.ConfigurationManager;
import com.apple.wwrc.foundation.configuration.impl.ConfigurationContext;
import com.apple.wwrc.foundation.configuration.impl.POSConfiguration;
import com.apple.wwrc.foundation.framework.Framework;
import com.apple.wwrc.foundation.framework.annotation.OnHealthCheck;
import com.apple.wwrc.foundation.framework.annotation.OnServiceStart;
import com.apple.wwrc.foundation.framework.annotation.OnServiceStop;
import com.apple.wwrc.foundation.framework.service.ServiceFactory;
import com.apple.wwrc.foundation.framework.exception.FrameworkException;
import com.apple.wwrc.service.customer.resolver.AbstractUserLookup;
import com.apple.wwrc.service.customer.resolver.DirectoryServiceResolver;
import com.apple.wwrc.service.customer.exception.InvalidInputException;
import com.apple.wwrc.service.customer.model.response.DSUser;
import com.apple.wwrc.service.customer.model.response.EmployeeSearch;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import java.util.regex.Pattern;


/**
 * CustomerService: new Home for POS-Customer.
 *
 */
public class CustomerService extends ServiceFactory implements CustomerServiceInterface
{
	//Use Framework for logging Facade.
	private static Logger logger = Framework.getLogger(CustomerService.class);
	private AbstractUserLookup customerLookup;
    private final Pattern VALID_EMAIL_REGEX = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);
    private POSConfiguration configurator;

	@OnServiceStart
	public void onStart() throws Exception {
        configurator = Framework.getConfigurationManager().getConfigurations("POS");
        setEnvAwait();
        customerLookup = createLookupResolver();
        logger.info("Service " + Framework.getServiceInfo(CustomerService.class) + " started...");
	}

    private void setEnvAwait() throws FrameworkException {
        String appId = null;
        String I3_env = null;
        String SSO_env = null;
        while (StringUtils.isBlank(appId)) {
            logger.info("pulling startup configurations...");
            try {
                Thread.sleep(1000);
                appId = configurator.getString(Constants.POS_APP_ID);
                I3_env = configurator.getString(Constants.I3_ENV);
                SSO_env = configurator.getString(Constants.SSO_ENV);
            } catch (InterruptedException e) {
                logger.info("Sleep interrupted. Shutdown the server.");
                System.exit(0);
            }
        }
        System.setProperty(Constants.I3_ENV, I3_env);
        System.setProperty(Constants.SSO_ENV, SSO_env);
        logger.info("Launching Environments:");
        logger.info(String.format("      %-10s : %s", "I3_ENV",I3_env));
        logger.info(String.format("      %-10s : %s", "SSO_ENV",SSO_env));
        logger.info(String.format("      %-10s : %s", "AppId",appId));
    }

	//For JUnit Test
    public DirectoryServiceResolver createLookupResolver() {
        return new DirectoryServiceResolver();
    }

    @OnServiceStop
	public void onStop() throws Exception {
        customerLookup.tearDown();
		logger.info("Service " + Framework.getServiceInfo(CustomerService.class) + " stopped...");
	}

	@OnHealthCheck
	public boolean onHealthCheck() {
		//everything fine.
		return true;
	}

    @Override
    public EmployeeSearch employeeLookup(String query, int page, int limit) throws FrameworkException {
        try {
            validateEmployeeLookupInput(query,page,limit);
        } catch (InvalidInputException invalid ) {
            logger.warn(String.format("employeeLookup(q=XXX, p=%d, l=%d) - %s",page,limit, invalid.getMessage()));
            return customerLookup.makeSearchResponse(page, limit, null);//Treat Invalid as Not Found
        }
        query = decryptPII(query);
        try {
            DSUser employee;//can be null
            if (isBadgeLookup(query)) {
                validateBadgeLookup(query);
                employee = customerLookup.lookupFromBadge(query, page, limit);
            } else {
                validateEmailLookup(query);
                employee = customerLookup.lookupFromEmail(query, page, limit);
            }
            //Response
            logger.info(String.format("employeeLookup(q=XXX, p=%d, l=%d) - OK",page,limit));

            return customerLookup.makeSearchResponse(page, limit, employee);
        } catch (InvalidInputException ie) {
            logger.warn(String.format("employeeLookup(q=XXX, p=%d, l=%d) - Invalid",page,limit));
            return customerLookup.makeSearchResponse(page, limit, null);//Treat Invalid as Not Found
        } catch (Exception e) {
            logger.error(String.format("employeeLookup(q=XXX, p=%d, l=%d) - Err",page,limit),e);
            throw new FrameworkException("Server Error: See " + CustomerService.class.getSimpleName() + "'s log");
        }
    }

    protected boolean isBadgeLookup(String query) {
	    return StringUtils.isNumeric(query);
    }

    protected String decryptPII(String ciferText) {
	    return ciferText;
    }

    private void validateBadgeLookup(String query) {
        //TODO Find regex for valid Badge ID.
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
