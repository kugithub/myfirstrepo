package com.apple.wwrc.service.customer.resolver;

import static org.junit.jupiter.api.Assertions.*;
import java.util.HashMap;
import java.util.Properties;
import com.apple.ist.retail.pos.encrypt.util.EncodeDecodeUtil;
import com.apple.ist.ds2.pub.common.DSRequestI;
import com.apple.ist.ds2.pub.person.PersonSearchCriteriaI;
import com.apple.ist.iss.client.ISObject;
import com.apple.ist.rpc2.RPCFactory;
import com.apple.wwrc.service.customer.model.response.DSUser;

import scala.Predef.any2stringadd;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Matchers;
import org.powermock.api.mockito.PowerMockito;

class DirectoryServiceResolverTest {

    private static DirectoryServiceResolver resolver;
    private static DSRequestI dsreq;

    @Test
    public void test() throws Exception {

    }

    public static HashMap<String, ISObject> objMap = new HashMap<String, ISObject>();

    @BeforeAll
    public static void setUp() throws Exception {
        //DOMCon
        System.setProperty("com.apple.wwrc.db.jdbcUrl","jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=tcp)(HOST=falkar.corp.apple.com)(PORT=1871))(CONNECT_DATA=(SERVICE_NAME=nexus2d)))");
        System.setProperty("com.apple.wwrc.db.user", "nexus_pod_user");
        System.setProperty("com.apple.wwrc.db.password",  EncodeDecodeUtil.encode("podnnexususerpassword"));
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
        JamaicaProxy mocked = PowerMockito.mock(JamaicaProxy.class);
        PowerMockito.doReturn(-1).when(mocked).getEncryptionType(Matchers.anyString());
        PowerMockito.doReturn("i6v9unmmmddx2i9r").when(mocked).decryptText(Matchers.anyString(),Matchers.eq(-1));
        
        resolver = PowerMockito.spy(new DirectoryServiceResolver());
        PowerMockito.doReturn(mocked).when(resolver).jamaicaProxy();
        try {
            dsreq = resolver.getDSRequest("113");
        } catch (Exception e) {
            //Ignore in Rio
        }
    }

    @Test
    public void testGetIDMSPassword() throws Exception {
        //Given + When + Then
        String decryptedPwd = resolver.getIDMSPassword("113");
        assertNotNull(decryptedPwd);
       
    }

    @Test
    public void testFindDSUserByEmail() throws Exception {
        //Given
        PersonSearchCriteriaI criteria  = (PersonSearchCriteriaI )RPCFactory.getData(PersonSearchCriteriaI.class);
        criteria.setEmail(new String[]{"sarora@apple.com"});
        criteria.setPrsTypeCode(new Integer[] {1});
        //When
        Long prsId = resolver.findPerson(dsreq, criteria);
        //Then
        assertEquals(new Long(1334518862), prsId);
    }

    @Test
    public void testFindDSUserByBadgeId() throws Exception {
        //Given
        PersonSearchCriteriaI criteria  = (PersonSearchCriteriaI )RPCFactory.getData(PersonSearchCriteriaI.class);
//        criteria.setEmail(new String[]{"npoolsappasit@apple.com"});
        criteria.setBadgeNumbers(new String[] {"532800"});
        criteria.setPrsTypeCode(new Integer[] {1});
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
        assertEquals(6484,userWithPhotoImage.getPhoto().getHighResPhoto().length());
        assertEquals("JPG",userWithPhotoImage.getPhoto().getHighResPhotoType());
        assertEquals(2516, userWithPhotoImage.getPhoto().getLowResPhoto().length());
        assertEquals("JPG",userWithPhotoImage.getPhoto().getLowResPhotoType());
        assertNotNull(userWithPhotoImage.getPtype());
        //When + Then
        DSUser userWithHisCubicleMapInsteadOfBadgePhoto = resolver.fetchPerson(dsreq, new Long(269574376));
        assertEquals("116352", userWithHisCubicleMapInsteadOfBadgePhoto.getBadgeID());
        assertNotNull(userWithHisCubicleMapInsteadOfBadgePhoto.getFirstName());
        assertNotNull(userWithHisCubicleMapInsteadOfBadgePhoto.getLastName());
        assertNotNull(userWithHisCubicleMapInsteadOfBadgePhoto.getFullName());
        assertNotNull(userWithHisCubicleMapInsteadOfBadgePhoto.getEmail());
        assertEquals("",userWithHisCubicleMapInsteadOfBadgePhoto.getPhoto().getHighResPhoto());
        assertEquals("",userWithHisCubicleMapInsteadOfBadgePhoto.getPhoto().getLowResPhoto());
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
        DSUser mk = resolver.lookupFromEmail("m.k@apple.com", 0, 10);
        assertEquals("Ben Mazin", ben.getFullName());
        assertEquals("MK Khan", mk.getFullName());
    }
}