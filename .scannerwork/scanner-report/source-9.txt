package com.apple.wwrc.service.customer.resolver;

import com.apple.ist.ds2.pub.common.AppCredentialsI;
import com.apple.ist.ds2.pub.common.DSRequestI;
import com.apple.ist.ds2.pub.common.EmailI;
import com.apple.ist.ds2.pub.common.NameI;
import com.apple.ist.ds2.pub.common.PersonCoreI;
import com.apple.ist.ds2.pub.person.PersonI;
import com.apple.ist.ds2.pub.person.PersonRequestI;
import com.apple.ist.ds2.pub.person.PersonResponseI;
import com.apple.ist.ds2.pub.person.PersonSearchCriteriaI;
import com.apple.ist.ds2.pub.person.PersonServiceI;
import com.apple.ist.iss.client.ISObject;
import com.apple.ist.retail.pos.encrypt.util.EncodeDecodeUtil;
import com.apple.ist.rpc2.RPCFactory;
import com.apple.wwrc.foundation.framework.exception.FrameworkException;
import com.apple.wwrc.foundation.security.model.input.EncryptDecryptText;
import com.apple.wwrc.foundation.security.service.SecurityInterface;
import com.apple.wwrc.service.customer.model.response.DSUser;
import com.apple.wwrc.service.customer.util.ServiceFactoryProxy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

class DirectoryServiceResolverTest {

    private static DirectoryServiceResolver resolver;
    private static DSRequestI dsreq;

    @Test
    public void test() throws Exception {

    }

    public static HashMap<String, ISObject> objMap = new HashMap<String, ISObject>();
    private static PersonServiceI mockPSI;
    private static PersonResponseI mockResponse;

    @BeforeAll
    public static void setUp() throws Exception {
        //DOMCon
        System.setProperty("com.apple.wwrc.db.jdbcUrl", "jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=tcp)(HOST=falkar.corp.apple.com)(PORT=1871))(CONNECT_DATA=(SERVICE_NAME=nexus2d)))");
        System.setProperty("com.apple.wwrc.db.user", "nexus_pod_user");
        System.setProperty("com.apple.wwrc.db.password", EncodeDecodeUtil.encode("podnnexususerpassword"));
        System.setProperty("com.apple.wwrc.db.type", "ORACLE");
        System.setProperty("wwrc.pos.ssl.config", "src/test/resources/test_ssl_properties");
        Properties env = System.getProperties();
        env.setProperty("bootStrapIp", "ws://localhost:9000/rcm");
        env.setProperty("id_groups", "POS");
        System.setProperty("SSO_ENV", "IDMS-UAT-HTTPS");
        System.setProperty("I3_ENV", "IDMS-UAT-HTTPS");
        System.setProperty("enablingDSLog", "true");
        env.setProperty("RPC", "false");//Got to be false to avoid loading fake keystore --> fake keystore will fail the Jamaica-Security
        //JamaicaProxy is meant to bypass the Jamaica Sandbox. This will allow RIO test to work w/o sandbox
        SecurityInterface mocked = mock(SecurityInterface.class);
        when(mocked.getEncryptionType(Matchers.anyString())).thenReturn(-1);
        when(mocked.decryptText(Matchers.any(EncryptDecryptText.class))).thenReturn("i6v9unmmmddx2i9r");
        ServiceFactoryProxy.stubb(SecurityInterface.class, mocked);

        resolver = new DirectoryServiceResolver();
        try {
            dsreq = resolver.getDSRequest("113");
        } catch (Exception e) {
            //Ignore in Rio
        }
    }

    @AfterEach
    void cleanupAfterEach() {
        ServiceFactoryProxy.unstubb();
    }

    @Test
    public void testHandleExceptionIngetDSRequest() throws Exception {
        //Given
        SecurityInterface mocked = mock(SecurityInterface.class);
        when(mocked.getEncryptionType(Matchers.anyString())).thenReturn(-1);
        when(mocked.decryptText(Matchers.any(EncryptDecryptText.class))).thenReturn(null);
        ServiceFactoryProxy.stubb(SecurityInterface.class, mocked);
        //When
        DirectoryServiceResolver failedResolver = new DirectoryServiceResolver();
        assertThrows(Exception.class, () -> failedResolver.getDSRequest("113"));
    }

    @Test
    public void testFindDSUserByEmail() throws Exception {
        //Given
        PersonSearchCriteriaI criteria = (PersonSearchCriteriaI) RPCFactory.getData(PersonSearchCriteriaI.class);
        criteria.setEmail(new String[]{"sarora@apple.com"});
        criteria.setPrsTypeCode(new Integer[]{1});
        //When
        Long prsId = resolver.findPerson(dsreq, criteria);
        //Then
        assertEquals(new Long(1334518862), prsId);
    }

    @Test
    public void testFindDSUserByBadgeId() throws Exception {
        //Given
        PersonSearchCriteriaI criteria = (PersonSearchCriteriaI) RPCFactory.getData(PersonSearchCriteriaI.class);
//        criteria.setEmail(new String[]{"npoolsappasit@apple.com"});
        criteria.setBadgeNumbers(new String[]{"532800"});
        criteria.setPrsTypeCode(new Integer[]{1});
        //When
        Long prsId = resolver.findPerson(dsreq, criteria);
        //Then
        assertEquals(new Long(973645158), prsId);
    }

    @Test
    public void testFetchDSUser() throws Exception {
        //When + Then
        DSUser userWithPhotoImage = resolver.fetchPerson(dsreq, new Long(1334518862));
        assertNotNull(userWithPhotoImage);
        assertEquals("143188", userWithPhotoImage.getBadgeID());
        assertNotNull(userWithPhotoImage.getFirstName());
        assertNotNull(userWithPhotoImage.getLastName());
        assertNotNull(userWithPhotoImage.getFullName());
        assertNotNull(userWithPhotoImage.getEmail());
        assertEquals(6484, userWithPhotoImage.getPhoto().getHighResPhoto().length());
        assertEquals("JPG", userWithPhotoImage.getPhoto().getHighResPhotoType());
        assertEquals(2516, userWithPhotoImage.getPhoto().getLowResPhoto().length());
        assertEquals("JPG", userWithPhotoImage.getPhoto().getLowResPhotoType());
        assertNotNull(userWithPhotoImage.getPtype());
        //When + Then
        DSUser userWithHisCubicleMapInsteadOfBadgePhoto = resolver.fetchPerson(dsreq, new Long(269574376));
        assertEquals("116352", userWithHisCubicleMapInsteadOfBadgePhoto.getBadgeID());
        assertNotNull(userWithHisCubicleMapInsteadOfBadgePhoto.getFirstName());
        assertNotNull(userWithHisCubicleMapInsteadOfBadgePhoto.getLastName());
        assertNotNull(userWithHisCubicleMapInsteadOfBadgePhoto.getFullName());
        assertNotNull(userWithHisCubicleMapInsteadOfBadgePhoto.getEmail());
        assertEquals("", userWithHisCubicleMapInsteadOfBadgePhoto.getPhoto().getHighResPhoto());
        assertEquals("", userWithHisCubicleMapInsteadOfBadgePhoto.getPhoto().getLowResPhoto());
        assertNotNull(userWithHisCubicleMapInsteadOfBadgePhoto.getPtype());
    }

    @Test
    public void testEmailLookup() throws Exception {
        DSUser u = resolver.lookupFromEmail("sarora@apple.com", 0, 10);
        assertNotNull(u);
    }

    @Test
    public void testBadgeLookup() throws Exception {
        DSUser u = resolver.lookupFromBadge("532800", 0, 10);
        assertNotNull(u);
    }

    @Test
    public void testBadgeLookup2() throws Exception {
        DSUser u = resolver.lookupFromBadge("12345", 0, 10);
        DSUser v = resolver.lookupFromBadge("215914", 0, 10);
        assertNotNull(u);
        assertNotNull(v);
    }

    @Test
    public void testExpectedPreferredName() throws Exception {
        DSUser ben = resolver.lookupFromEmail("mazin.b@apple.com", 0, 10);
        assertEquals("Ben Mazin", ben.getFullName());
    }

    @Test
    public void testFindPersonHandleIDMSException() throws Exception {
        //Given
        mockPSI = mock(PersonServiceI.class);
        when(mockPSI.findPerson(Matchers.any(PersonRequestI.class))).thenThrow(new Exception("Oh *it"));
        ServiceFactoryProxy.stubb(PersonServiceI.class, mockPSI);
        //When
        PersonSearchCriteriaI criteria = (PersonSearchCriteriaI) RPCFactory.getData(PersonSearchCriteriaI.class);
        criteria.setEmail(new String[]{"sarora@apple.com"});
        criteria.setPrsTypeCode(new Integer[]{1});
        //When
        Exception e = assertThrows(FrameworkException.class, () -> resolver.findPerson(dsreq, criteria));
        assertEquals("Oh *it", e.getMessage());
    }

    @Test
    public void testFetchDSUserHandleIDMSException() throws Exception {
        //Given
        mockPSI = mock(PersonServiceI.class);
        when(mockPSI.fetchPerson(Matchers.any(PersonRequestI.class))).thenThrow(new Exception("Holy it"));
        ServiceFactoryProxy.stubb(PersonServiceI.class, mockPSI);
        //When + Then
        Exception e = assertThrows(FrameworkException.class, () -> resolver.fetchPerson(dsreq, new Long(1334518862)));
        assertEquals("Holy it", e.getMessage());
    }

    @Test
    public void testGetPreferedName() throws Exception {
        //Given 1
        NameI nm1 = mock(NameI.class);
        when(nm1.getFirstName()).thenReturn("fName");
        when(nm1.getLastName()).thenReturn("lName");
        when(nm1.getNickName()).thenReturn("nicky");
        //When + Then
        PersonCoreI pk = mock(PersonCoreI.class);
        when(pk.getName()).thenReturn(nm1);
        assertEquals("nicky", resolver.getPreferedName(pk));
        //When
        NameI nm2 = mock(NameI.class);
        when(nm2.getFirstName()).thenReturn("fName");
        when(nm2.getLastName()).thenReturn("lName");
        when(nm2.getNickName()).thenReturn(null);
        when(pk.getName()).thenReturn(nm2);
        assertEquals("fName", resolver.getPreferedName(pk));
    }

    @Test
    public void testGetEmailAddrFallBcktoNullIfNotHave() throws Exception {
        //Given:
        PersonI mP = mock(PersonI.class);
        PersonCoreI pc = mock(PersonCoreI.class);
        when(mP.getPersonCoreInfo()).thenReturn(pc);
        EmailI email1 = mock(EmailI.class);
        EmailI email2 = mock(EmailI.class);
        when(email1.getEmailAddress()).thenReturn("primary email");
        when(email1.getEmailTypeCode()).thenReturn(1);
        when(email2.getEmailAddress()).thenReturn("additional email");
        when(email2.getEmailTypeCode()).thenReturn(2);
        EmailI[] additionalEmails = (EmailI[]) Arrays.asList(email2).toArray();

        //When: Had both primary email and additional email
        when(pc.getEmail()).thenReturn(email1);
        when(mP.getAdditionalEmails()).thenReturn(additionalEmails);
        assertEquals("primary email", resolver.getEmailAddress(mP));

        //When: Has only additional email
        when(pc.getEmail()).thenReturn(null);
        when(mP.getAdditionalEmails()).thenReturn(additionalEmails);
        assertEquals("additional email", resolver.getEmailAddress(mP));

        //When: Has no email
        when(pc.getEmail()).thenReturn(null);
        when(mP.getAdditionalEmails()).thenReturn(null);
        assertEquals("", resolver.getEmailAddress(mP));
    }

    @Test
    public void testGetIDMSPasswordNotFound() {
        String invalid_app_id = "invalid_app_id";
        assertThrows(Exception.class,
                () -> new DirectoryServiceResolver().getIDMSPassword(invalid_app_id));
    }

    @Test
    public void testGetDSUserFromResponseNoResult() throws NoSuchMethodException {
        DirectoryServiceResolver resolver = new DirectoryServiceResolver();
        Method getDSUserFromResponse = resolver.getClass().getDeclaredMethod("getDSUserFromResponse",
                PersonResponseI.class, long.class);
        getDSUserFromResponse.setAccessible(true);

        PersonResponseI mockedPersonResponseI = Mockito.mock(PersonResponseI.class);
        Mockito.when(mockedPersonResponseI.getPersons()).thenReturn(null);

        assertThrows(Exception.class,
                () -> getDSUserFromResponse.invoke(resolver, mockedPersonResponseI, 1L));

    }

    @Test
    public void testTearDown() throws NoSuchFieldException, IllegalAccessException {
        DirectoryServiceResolver resolver = new DirectoryServiceResolver();
        Field idmsCache = resolver.getClass().getDeclaredField("idmsCache");
        idmsCache.setAccessible(true);
        ConcurrentMap<String, AppCredentialsI> map = (ConcurrentMap<String, AppCredentialsI>) idmsCache.get(resolver);

        assertEquals(0, map.size());

        map.put("someKey", (AppCredentialsI) RPCFactory.getData(AppCredentialsI.class.getName()));
        assertEquals(1, map.size());

        resolver.tearDown();
        assertEquals(0, map.size());
    }

}