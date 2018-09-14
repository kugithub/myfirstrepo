package com.apple.wwrc.service.customer.resolver;

import com.apple.ist.ds2.pub.common.AppCredentialsI;
import com.apple.ist.ds2.pub.common.DSRequestI;
import com.apple.ist.ds2.pub.common.EmailI;
import com.apple.ist.ds2.pub.common.PersonCoreI;
import com.apple.ist.ds2.pub.common.PersonTypeI;
import com.apple.ist.ds2.pub.person.EmployeeFetchPrefsI;
import com.apple.ist.ds2.pub.person.PersonFetchPrefsI;
import com.apple.ist.ds2.pub.person.PersonI;
import com.apple.ist.ds2.pub.person.PersonRequestI;
import com.apple.ist.ds2.pub.person.PersonResponseI;
import com.apple.ist.ds2.pub.person.PersonSearchCriteriaI;
import com.apple.ist.ds2.pub.person.PersonServiceI;
import com.apple.ist.ds2.pub.person.PhotoI;
import com.apple.ist.rpc2.RPCFactory;
import com.apple.ist.rpc2.util.ObjectLog;
import com.apple.wwrc.foundation.configuration.impl.ConfigurationContext;
import com.apple.wwrc.foundation.configuration.impl.POSConfiguration;
import com.apple.wwrc.foundation.framework.Framework;
import com.apple.wwrc.foundation.framework.exception.FrameworkException;
import com.apple.wwrc.foundation.framework.identify.exception.AuthenticationException;
import com.apple.wwrc.foundation.framework.util.FoundationCache;
import com.apple.wwrc.foundation.security.builder.SecurityRequestBuilder;
import com.apple.wwrc.foundation.security.service.EncryptionType;
import com.apple.wwrc.foundation.security.service.SecurityInterface;
import com.apple.wwrc.service.customer.Constants;
import com.apple.wwrc.service.customer.model.response.CustomerPhoto;
import com.apple.wwrc.service.customer.model.response.DSUser;
import com.apple.wwrc.service.customer.util.ServiceFactoryProxy;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import java.util.Base64;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
@SuppressWarnings({"squid:S00103","common-java:InsufficientCommentDensity","squid:S1166","squid:S1162","squid:S109", "squid:S00112"})
public class DirectoryServiceResolver extends AbstractUserLookup {
    public static final long NOT_FOUND = -1L;
    public static final int APPLE_EMPLOYEE = 1;
    public static final int APPLE_CONTRACTOR = 2;
    public static final int INDEPENDENT_CONTRACTOR = 3;
    public static final int ONSITE_VENDER = 4;
    public static final int VENDER = 6;
    private static final long FOUNDATION_CACHE_DURATION = 12L;
    private static final int FOUNDATION_CACHE_MAX_SIZE = 10;

    private static final int EMAIL_TYPE_OFFICIAL = 1;
    private static final int EMAIL_TYPE_PREFERED = 2;
    private static final int BADGE_PHOTO = 1;
    private static final int THUMBNAIL_PHOTO = 2;
    private static final int CATEGORY_PHOTO = 1;
    private static final Logger LOGGER = Framework.getLogger(DirectoryServiceResolver.class);
    private boolean showIDMSlog = false;
    private POSConfiguration configurator;
    private String appId;
    private ConcurrentMap<String, AppCredentialsI> idmsCache;

    public DirectoryServiceResolver() {
        ConfigurationContext cntxt = new ConfigurationContext("POS");
        configurator = Framework.getConfigurationManager().getConfigurations(cntxt);
        showIDMSlog = "true".equalsIgnoreCase(System.getProperty("enablingDSLog", "false"));
        appId = configurator.getString(Constants.POS_APP_ID);
        idmsCache = FoundationCache.createExpiringMap(FOUNDATION_CACHE_DURATION, TimeUnit.HOURS, FOUNDATION_CACHE_MAX_SIZE);
    }

    @Override
    public void tearDown() {
        if (null != idmsCache) {
            idmsCache.clear();
        }
    }

    public String getIDMSPassword(String appId) throws Exception {
        LOGGER.debug("getIDMSPassword({})", appId);
        String encryptedPsswd = configurator.getString(String.format("%s.%s", appId, Constants.VAR_1));
        if (StringUtils.isBlank(encryptedPsswd)) {
            throw new Exception("IDMS password not found for " + appId);
        }
        SecurityInterface jamaicaSecurity = ServiceFactoryProxy.createJamaicaSecurityClient();
        int encryptionType = jamaicaSecurity.getEncryptionType(encryptedPsswd);
        String decryptedPwd = jamaicaSecurity.decryptText(SecurityRequestBuilder.getEncryptDecryptText(
                EncryptionType.typeLookup(encryptionType), null,
                encryptedPsswd, "", "", ""));
        if (StringUtils.isBlank(decryptedPwd)) {
            throw new AuthenticationException("Decrypted password is empty for key : " + String.format("%s.%s", appId, Constants.VAR_1));
        }
        return decryptedPwd;
    }

    @Override
    public DSUser lookupFromEmail(String email, int page, int limit) throws Exception {
        //Create RPC Request
        DSRequestI dsreq = getDSRequest(appId);
        //Setup Search Criteria
        PersonSearchCriteriaI personSC = (PersonSearchCriteriaI) RPCFactory.getData(PersonSearchCriteriaI.class);
        personSC.setEmail(new String[]{email});
        personSC.setPrsTypeCode(new Integer[]{APPLE_EMPLOYEE, APPLE_CONTRACTOR, ONSITE_VENDER, VENDER, INDEPENDENT_CONTRACTOR});

        Long dsId = findPerson(dsreq, personSC);
        if (dsId == NOT_FOUND) {
            return null;
        }
        return fetchPerson(dsreq, dsId);
    }

    @Override
    public DSUser lookupFromBadge(String badgeId, int page, int limit) throws Exception {
        //Create RPC Request
        DSRequestI dsreq = getDSRequest(appId);
        //Setup Search Criteria
        PersonSearchCriteriaI personSC = (PersonSearchCriteriaI) RPCFactory.getData(PersonSearchCriteriaI.class);
        personSC.setBadgeNumbers(new String[]{badgeId});
        personSC.setPrsTypeCode(new Integer[]{APPLE_EMPLOYEE, APPLE_CONTRACTOR, ONSITE_VENDER, VENDER, INDEPENDENT_CONTRACTOR});

        Long dsId = findPerson(dsreq, personSC);
        if (dsId == NOT_FOUND) {
            return null;
        }
        return fetchPerson(dsreq, dsId);
    }

    protected synchronized AppCredentialsI getDSCredential(String appId) throws Exception {
        AppCredentialsI cred = idmsCache.get(appId);
        if (cred == null) {
            cred = createDSCredential(appId);
            idmsCache.put(appId, cred);
        }
        return cred;
    }

    private AppCredentialsI createDSCredential(String appId) throws Exception {
        String appPassword = getIDMSPassword(appId);
        AppCredentialsI acred = (AppCredentialsI) RPCFactory.getData(AppCredentialsI.class.getName());
        acred.setAppID(new Long(appId));
        acred.setAppPassword(appPassword);
        return acred;
    }

    /**
     * Setting the PersonRequest with APP Credentials
     *
     * @return
     */
    protected DSRequestI getDSRequest(String appId) throws Exception {
        try {
            AppCredentialsI acred = getDSCredential(appId);
            DSRequestI dsreq = (DSRequestI) RPCFactory.getData(DSRequestI.class);
            dsreq.setAppCredentials(acred);
            if (showIDMSlog) {
                ObjectLog.printObject(dsreq);
            }
            return dsreq;
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw e;
        }
    }

    public Long findPerson(DSRequestI dsreq, PersonSearchCriteriaI criteria) throws Exception {
        //Given
        PersonRequestI findReqI = (PersonRequestI) RPCFactory.getData(PersonRequestI.class);
        findReqI.setDSRequest(dsreq);
        PersonI prs = (PersonI) RPCFactory.getData(PersonI.class.getName());
        findReqI.setPerson(prs);
        findReqI.setPersonSearchCriteria(criteria);

        //Execute
        PersonResponseI response = null;
        try {
            PersonServiceI directorySearchServiceIF = ServiceFactoryProxy.createPersonServiceI();
            response = directorySearchServiceIF.findPerson(findReqI);
        } catch (Exception re) {
            //This will allow remote exception to show where it is occurred.
            throw new FrameworkException(re.getMessage());
        } finally {
            if (showIDMSlog) {
                ObjectLog.printObject(findReqI);
                ObjectLog.printObject(response);
            }
        }
        //Parse result
        if (response.getPersons() == null || response.getPersons()[0] == null) {
            return NOT_FOUND;
        }
        //every thing else should be server error
        return response.getPersons()[0].getPersonCoreInfo().getPersonID();
    }

    public DSUser fetchPerson(DSRequestI dsreq, Long personID) throws Exception {
        //Given: Setup fetch preferences
        EmployeeFetchPrefsI epf = (EmployeeFetchPrefsI) RPCFactory.getData(EmployeeFetchPrefsI.class);
        epf.setFetchBadge(true);
        epf.setFetchChat(false);
        epf.setFetchEmployeePrefs(false);
        epf.setFetchOfficeLocation(false);
        epf.setFetchPhoto(true);
        epf.setFetchRelations(false);
        PersonFetchPrefsI pf = (PersonFetchPrefsI) RPCFactory.getData(PersonFetchPrefsI.class);
        pf.setEmployeeFetchPrefs(epf);
        pf.setFetchAddress(false);
        pf.setFetchCompany(false);
        pf.setFetchCoreInfo(true);
        pf.setFetchCustomer(true);
        pf.setFetchEmployee(false);
        pf.setFetchPhone(false);
        pf.setFetchAccount(true);
        pf.setFetchOwner(true);
        pf.setFetchProfile(true);
        PersonRequestI fetchReq = createFetchRequest(dsreq, personID, pf);

        //Execute
        PersonResponseI response = null;
        try {
            PersonServiceI directorySearchServiceIF = ServiceFactoryProxy.createPersonServiceI();
            response = directorySearchServiceIF.fetchPerson(fetchReq);
        } catch (Exception re) {
            throw new FrameworkException(re.getMessage());//This will allow remote exception to show where it is occurred.
        } finally {
            if (showIDMSlog) {
                ObjectLog.printObject(fetchReq);
                ObjectLog.printObject(response);
            }
        }
        return getDSUserFromResponse(response, personID);
    }

    private DSUser getDSUserFromResponse(PersonResponseI response, long personID) throws Exception {
        if (null == response.getPersons()) {
            throw new Exception("Can't fetch person given id=" + personID);
        }
        //Given a valid personID from findPerson, NPE should not happen
        PersonI person = response.getPersons()[0];
        String dsId = person.getPersonCoreInfo().getPersonID().toString();
        String fName = getPreferedName(person.getPersonCoreInfo());
        String lName = person.getPersonCoreInfo().getName().getLastName();
        String badgeID = person.getEmployee().getBadge().getBadgeID();
        PersonTypeI pType = person.getPersonCoreInfo().getPrsType()[0];
        String email = getEmailAddress(person);
        DSUser dsUser = new DSUser(dsId, fName, lName, email, badgeID, pType.getTypeCode());

        //Parse Photo
        PhotoI[] photos = person.getEmployee().getPhoto();
        CustomerPhoto image = dsUser.getPhoto();
        if (photos != null && photos.length > 0) {
            for (PhotoI p : photos) {
                byte[] rawImage = p.getImage();
                if (p.getImageCategoryCode() != CATEGORY_PHOTO) {
                    //Only parse Photo Image.
                    continue;
                }
                if (p.getImageContentType() == THUMBNAIL_PHOTO) {
                    image.setLowResPhoto(Base64.getEncoder().encodeToString(rawImage));
                    image.setLowResPhotoType(p.getImageFormatName());
                } else if (p.getImageContentType() == BADGE_PHOTO) {
                    image.setHighResPhoto(Base64.getEncoder().encodeToString(rawImage));
                    image.setHighResPhotoType(p.getImageFormatName());
                } else {
                    //Do nothing as they can be MAP CUBE image
                }
            }
        }
        return dsUser;
    }

    protected String getPreferedName(PersonCoreI personCoreI) {
        String preferedName = personCoreI.getName().getNickName();
        if (StringUtils.isBlank(preferedName)) {
            preferedName = personCoreI.getName().getFirstName();
        }
        return preferedName;
    }

    /**
     * Try get email address from primary email and fallback to additional email.
     *
     * @return primary email address or null if not found.
     */
    protected String getEmailAddress(PersonI person) {
        EmailI primaryEmail = person.getPersonCoreInfo().getEmail();
        if (primaryEmail != null && !StringUtils.isBlank(primaryEmail.getEmailAddress())) {
            return primaryEmail.getEmailAddress();
        }
        //Secondary email
        EmailI[] emails = person.getAdditionalEmails();
        if (emails != null && emails.length > 0) {
            for (EmailI email : emails) {
                if (email.getEmailTypeCode() == EMAIL_TYPE_OFFICIAL || email.getEmailTypeCode() == EMAIL_TYPE_PREFERED) {
                    return email.getEmailAddress();
                }
            }
        }
        LOGGER.debug("WARN: This person [{}] has no Official or Preferred email", person);
        return "";
    }

    private PersonRequestI createFetchRequest(DSRequestI dsreq, Long personID, PersonFetchPrefsI preference) {
        // create request object and assign the session as session returned from auth api call
        PersonRequestI fetchReqI = (PersonRequestI) RPCFactory.getData(PersonRequestI.class.getName());
        fetchReqI.setDSRequest(dsreq);

        // create Person,PersonCore object and pass the prsid
        PersonI prs = (PersonI) RPCFactory.getData(PersonI.class.getName());
        prs.setUpdated(true);
        PersonCoreI pc = (PersonCoreI) RPCFactory.getData(PersonCoreI.class.getName());
        pc.setUpdated(true);
        pc.setPersonID(personID);// PASS PERSONID
        prs.setPersonCoreInfo(pc);
        fetchReqI.setPerson(prs);
        fetchReqI.setPersonFetchPrefs(preference);
        return fetchReqI;
    }
}
