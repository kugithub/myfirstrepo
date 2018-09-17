package com.apple.wwrc.service.authorization;

import com.apple.ist.retail.pos.encrypt.util.EncodeDecodeUtil;
import com.apple.wwrc.foundation.configuration.impl.ConfigurationManagerImpl;
import com.apple.wwrc.foundation.framework.identify.exception.AuthorizationException;
import com.apple.wwrc.foundation.framework.ssl.SSLFactory;
import com.apple.wwrc.foundation.framework.util.JsonUtils;
import com.apple.wwrc.foundation.framework.util.PlatformConstants;
import com.apple.wwrc.foundation.security.util.JWTUtils;
import com.apple.wwrc.service.authorization.repository.impl.UserAccessPolicyRepositoryImpl;
import org.junit.jupiter.api.*;
import org.mockito.Matchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.PrintWriter;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.powermock.api.mockito.PowerMockito.*;

class AuthorizationServiceTest {
  private static final int JOE_THE_STORE_ASSOCIATE = 55795;
  private static AuthorizationService service;
  protected static final Logger logger = LoggerFactory.getLogger(AuthorizationServiceTest.class);

  @BeforeAll
  public static void setupBeforeTest() throws Exception {
    //oracle
    System.setProperty("com.apple.wwrc.db.jdbcUrl",
      "jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=tcp)(HOST=falkar.corp.apple.com)(PORT=1871))(CONNECT_DATA=(SERVICE_NAME=nexus2d)))");
    System.setProperty("com.apple.wwrc.db.user", "nexus_user");
    System.setProperty("com.apple.wwrc.db.password", EncodeDecodeUtil.encode("xyonnexususerpassword"));
    System.setProperty("com.apple.wwrc.db.type", "ORACLE");
    System.setProperty("id_groups", "POS");
    System.setProperty("RPC", "false");
    System.setProperty("bootStrapIp", "ws://localhost:9000/rcm");

    String cipher = EncodeDecodeUtil.encode("password");
    String cipherKey = EncodeDecodeUtil.encode("thisKeyIsVeryVerySecure_");
    new File("src/test/resources/test_ssl_properties").delete();
    try (PrintWriter writer = new PrintWriter("src/test/resources/test_ssl_properties")) {
      writer.println("wwrc.pos.secure=true");
      writer.println("com.apple.ist.retail.pos.security.keyStoreFile=src/test/resources/test-keystore.jks");
      writer.println("com.apple.ist.retail.pos.security.keyStorePassword=" + cipher);
      writer.println("com.apple.ist.retail.pos.security.trustStoreFile=src/test/resources/test-truststore.jks");
      writer.println("com.apple.ist.retail.pos.security.trustStorePassword=" + cipher);
      writer.println("com.apple.ist.retail.pos.security.jwtKey=" + cipherKey);
      writer.flush();
    }
    System.setProperty("wwrc.pos.ssl.config", "src/test/resources/test_ssl_properties");
    SSLFactory.getInstance();//JWTUtil use this instnace.
    service = new AuthorizationService();
    service.onStart();
    System.out.println("Start AuthorizationService Test...");
  }

  @BeforeEach
  public void stubbUpBeforeTest() {
    //        ServiceFactoryProxy.stubb(SSLFactory.class, mockSSLFactory);
  }

  @AfterEach
  public void cleanupAfterTest() {
    ServiceFactoryProxy.unstubb();
  }

  @AfterAll
  public static void tearDownAfterTest() {
    try {
      service.onStop();
      ConfigurationManagerImpl.tearDown();
      new File("src/test/resources/test_ssl_properties").deleteOnExit();
    } catch (Exception e) {
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  @Test
  void testFetchRoleInfoFromHalfJsonString() throws Exception {
    //Given
    String roles = "[{\"role\":\"3\",\"storeId\":\"R043\",\"saleOrg\":\"5630\"},"
      + "{\"role\":\"3\",\"storeId\":\"T\",\"saleOrg\":\"5630\"},"
      + "{\"role\":\"3\",\"storeId\":\"P\",\"saleOrg\":\"5630\"}]";
    //When
    String jsonDecorated = "{\"roles\":" + roles + "}";
    Map result = JsonUtils.fromJson(jsonDecorated, Map.class);
    List<Map> x = (List<Map>) result.get("roles");
    Iterator<Map> y = x.iterator();
    //Then
    while (y.hasNext()) {
      Map map = (Map) y.next();
      System.out.println("role:" + map.get("role"));
      System.out.println("saleOrg:" + map.get("saleOrg"));
      System.out.println("store:" + map.get("storeId"));
    }
  }

  @Test
  public void testMergePermissionString() throws Exception {
    //Merge permission
    assertEquals("rwx", service.merge(Arrays.asList("-w-", "r--", "--x", "---")));
    //Give Positive Rule
    assertEquals("r--", service.merge(Arrays.asList("r--", "---")));
    //Idempotent
    assertEquals("rw-", service.merge(Arrays.asList("r--", "rw-")));
  }

  @Test
  public void testGetPermissionsThrowDeprecateException() throws Exception {
    assertThrows(AuthorizationException.class, () -> service.getPermissions("identity:idms.group.1232172", "Configurator/v1.0"));
  }

  @Test
  public void testIsJwtAuthorized() throws Exception {
    //Given
    String Runner = "1290";
    Map<String, String> token = new HashMap<String, String>();
    token.put(PlatformConstants.APP_ID, Runner);
    token.put(PlatformConstants.CONTEXT_USER_ID, "1234");
    String jwtToken = JWTUtils.createToken(token);

    assertEquals(true, service.isJwtAuthorized(jwtToken, "Bag/v1.0", "expressCheckout"));
    assertEquals(true, service.isJwtAuthorized(jwtToken, "Bag/v1.0", "tenderForAPU"));
    assertEquals(true, service.isJwtAuthorized(jwtToken, "Bag/v1.0", "completeTransaction"));
    assertEquals(false, service.isJwtAuthorized(jwtToken, "Bag/v1.0", "login"));
    //Input validation
    Exception e1 = assertThrows(AuthorizationException.class, () -> service.isJwtAuthorized(jwtToken, "Bag/v1,000,000.0", "expressCheckout"));
    assertEquals("Authorization ERROR. see Authentication Server's Log", e1.getMessage());
    Exception e2 = assertThrows(AuthorizationException.class, () -> service.isJwtAuthorized(null, "Bag/v1.0", "expressCheckout"));
    assertEquals("Invalid Arguments for Authorization call", e2.getMessage());
    Exception e3 = assertThrows(AuthorizationException.class, () -> service.isJwtAuthorized(jwtToken, null, "expressCheckout"));
    assertEquals("Invalid Arguments for Authorization call", e3.getMessage());
    Exception e4 = assertThrows(AuthorizationException.class, () -> service.isJwtAuthorized(jwtToken, "Bag/v1.0", null));
    assertEquals("Invalid Arguments for Authorization call", e4.getMessage());
  }

  @Test
  public void testInvalidJwtTokenThrowException() throws Exception {
    //Given
    String invalidToken1 = "Invalid_JWT_Token";
    assertThrows(AuthorizationException.class, () -> service.isJwtAuthorized(invalidToken1, "Bag/v1,000,000.0", "expressCheckout"));
    //AppId validation
    Map<String, String> token = new HashMap<String, String>();
    token.put(PlatformConstants.APP_ID, "");
    token.put(PlatformConstants.CONTEXT_USER_ID, "1234");
    String invalidToken2 = JWTUtils.createToken(token);
    assertThrows(AuthorizationException.class, () -> service.isJwtAuthorized(invalidToken2, "Bag/v1.0", "expressCheckout"));
  }

  @Test
  public void testIsUidAuthorizedOnPlatformService() throws Exception {
    //Given
    String Bag = "identity:idms.group.1232161";
    assertEquals(true, service.isUidAuthorized(Bag, "Configuration/v1.0", "getConfigurationRowsPagination"));
    assertEquals(true, service.isUidAuthorized(Bag, "Configuration/v1.0", "DOMAIN:PLATFORM"));//Backward Compatibility
    assertEquals(true, service.isUidAuthorized(Bag, "Authorization/v1.0", "DOMAIN:PLATFORM"));//Backward Compatibility
    assertEquals(true, service.isUidAuthorized(Bag, "Authorization/v1.0", "isJWTAuthorized"));
    assertEquals(true, service.isUidAuthorized(Bag, "Authorization/v1.0", "futurePOSMethod"));
  }

  @Test
  public void testIsUidAuthorizedOnSameDomain() throws Exception {
    //Given
    String Authorization = "identity:idms.group.1232163";
    String ApiGateway = "identity:idms.group.1232171";
    String Bag = "identity:idms.group.1232161";
    assertEquals(true, service.isUidAuthorized(Authorization, "Configuration/v1.0", "getConfigurationRowsPagination"));
    assertEquals(true, service.isUidAuthorized(ApiGateway, "ApiGateway/v1.0", "login"));
    assertEquals(true, service.isUidAuthorized(Bag, "WebOrderManagement/v1.0", "DOMAIN:RETAIL"));
  }

  @Test
  public void testIsUidAuthorizedOnCrossDomain() throws Exception {
    //Given
    String Bag = "identity:idms.group.1232161";
    assertEquals(false, service.isUidAuthorized(Bag, "JamaicaSecurity/v1.0", "DOMAIN:SECURITY"));//No more domain access
    assertEquals(true, service.isUidAuthorized(Bag, "JamaicaSecurity/v1.0", "encryptText"));
    assertEquals(false, service.isUidAuthorized(Bag, "LegacyBridgeUtil/v1.0", "DOMAIN:LEGACY"));//No more domain access
    assertEquals(true, service.isUidAuthorized(Bag, "LegacyBridgeUtil/v1.0", "getStoreStatus"));
    assertEquals(true, service.isUidAuthorized(Bag, "LegacyBridgeUtil/v1.0", "createTransactionId"));
    assertEquals(true, service.isUidAuthorized(Bag, "LegacyBridgeUtil/v1.0", "getItemsByPartNumber"));
    assertEquals(true, service.isUidAuthorized(Bag, "LegacyBridgeUtil/v1.0", "publishTransaction"));
  }

  @Test
  public void testEasyPayAccess() throws Exception {
    //Given
    int USER = JOE_THE_STORE_ASSOCIATE;
    String STORE_IN_US = "R092";
    String STORE_ON_MAR = "STORE_ON_MAR";
    //When+Then
    assertTrue(service.hasEasyPayAccess(USER, STORE_IN_US));
    assertFalse(service.hasEasyPayAccess(USER, STORE_ON_MAR));
  }

  @Test
  public void testDSUSerAccess() throws Exception {
    Map<String, String> payload = new HashMap<>();
    payload.put(PlatformConstants.APP_ID, "1034");
    payload.put(PlatformConstants.CONTEXT_USER_ID, "1334518862");
    String jwtToken = JWTUtils.createToken(payload);

    service.validatePermission(jwtToken, "Chargeback Access");
    service.validateSalesOrgPermission(jwtToken, "5630", "Admin Access US");
    assertThrows(AuthorizationException.class, () -> service.validatePermission(jwtToken, "iPhone Sale Access"));
  }

  @Test
  public void testDieOnStartupIfJWTKeyNotFound() throws Exception {
    //Given
    SSLFactory factory = mock(SSLFactory.class);
    when(factory.getJWTKey()).thenReturn(null);
    ServiceFactoryProxy.stubb(SSLFactory.class, factory);
    //When
    AuthorizationService srv = new AuthorizationService();
    IllegalStateException e1 = assertThrows(IllegalStateException.class, () -> srv.onStart());
    assertEquals("JWT Encryption key is null or error. Check ssl.properties", e1.getMessage());
    //When
    when(factory.getJWTKey()).thenThrow(new IllegalStateException("What The Fantastic"));
    IllegalStateException e2 = assertThrows(IllegalStateException.class, () -> srv.onStart());
    assertEquals("What The Fantastic", e2.getMessage());
  }

  @Test
  public void testInputValidationIsUidAuthorized() throws Exception {
    //Given
    String Bag = "identity:idms.group.1232161";
    assertThrows(AuthorizationException.class, () -> service.isUidAuthorized(null, "Configuration/v1.0", "getConfigurationRowsPagination"));
    assertThrows(AuthorizationException.class, () -> service.isUidAuthorized(Bag, null, "getConfigurationRowsPagination"));
    assertThrows(AuthorizationException.class, () -> service.isUidAuthorized(Bag, "Configuration/v1.0", null));
    assertThrows(AuthorizationException.class, () -> service.isUidAuthorized(Bag, "Configuration/v100", "getConfigurationRowsPagination"));
  }

  @Test
  public void testUserBaseAccessControl() throws Exception {
    UserAccessPolicyRepositoryImpl repo = mock(UserAccessPolicyRepositoryImpl.class);
    doNothing().when(repo).updateRepo(Matchers.any(List.class));//Ignore POD DB. Use the following...
    when(repo.getPermissionString("2", "1583", "R092", "R092-Store-permission")).thenReturn("r-e");
    when(repo.getPermissionString("3", "1583", "R092", "R092-Store-permission")).thenReturn("-w-");
    when(repo.getPermissionString("3", "1583", "R095", "R095-Store-permission")).thenReturn("-we");
    when(repo.getPermissionString("3", "5650", "R999", "R999-Store-permission")).thenReturn("r--");
    when(repo.ready()).thenReturn(true);
    ServiceFactoryProxy.stubb(UserAccessPolicyRepositoryImpl.class, repo);
    AuthorizationService service_ = new AuthorizationService();
    service_.onStart();

    //When
    Map<String, String> token = new HashMap<String, String>();
    token.put(PlatformConstants.APP_ID, "113");
    token.put(PlatformConstants.CONTEXT_USER_ID, "" + JOE_THE_STORE_ASSOCIATE);
    String jwtToken = JWTUtils.createToken(token);

    //Then Global Permissions
    assertEquals("-w-", service_.getStorePermission(jwtToken, "1583", "R092", "R092-Store-permission"));
    assertEquals("-w-", service_.getStorePermission(jwtToken, null, null, "R092-Store-permission"));
    assertEquals("---", service_.getStorePermission(jwtToken, "5650", null, "R092-Store-permission"));//Asking for R092 permission from different Geo
    assertEquals("---", service_.getStorePermission(jwtToken, null, null, "R095-Store-permission"));//No permission on different store
    assertEquals("---", service_.getStorePermission(jwtToken, "5650", null, "R099-Store-permission"));//No permission on different Geo
    //Then
    service_.validatePermission(jwtToken, "R092-Store-permission");//Not throw exception
    service_.validateSalesOrgPermission(jwtToken, "1583", "R092-Store-permission");//Only know SalesOrg but actual does has clearance
    service_.validateStorePermission(jwtToken, "R092", "R092-Store-permission");//Only know storeId but actual does has clearance
    //Then
    assertThrows(AuthorizationException.class, () -> service_.validatePermission(jwtToken, "R095-Store-permission"));//Wrong store
    assertThrows(AuthorizationException.class, () -> service_.validateSalesOrgPermission(jwtToken, "1583", "R095-Store-permission"));//Wrong store
    assertThrows(AuthorizationException.class, () -> service_.validateSalesOrgPermission(jwtToken, "5650", "R092-Store-permission"));//Wrong SalesOrg
    assertThrows(AuthorizationException.class, () -> service_.validateStorePermission(jwtToken, "R092", "Permission-Not-Exist"));//Wrong permission
    //Then Exception tests
    assertThrows(AuthorizationException.class, () -> service_.validateStorePermission(jwtToken, "K000", "R092-Store-permission"));//No SalesOrg found
    token.put(PlatformConstants.CONTEXT_USER_ID, "NOT-A-NUMBER");
    String jwtToken_notNumber = JWTUtils.createToken(token);
    assertThrows(AuthorizationException.class, () -> service_.validateStorePermission(jwtToken_notNumber, "R092", "R092-Store-permission"));//DS_ID not a number
    token.put(PlatformConstants.CONTEXT_USER_ID, "");
    String jwtToken_noUserId = JWTUtils.createToken(token);
    assertThrows(AuthorizationException.class, () -> service_.validateStorePermission(jwtToken_noUserId, "R092", "R092-Store-permission"));//Missing DS_ID
  }
}
