package com.apple.wwrc.service.authorization.resolver;

import com.apple.ist.retail.pos.encrypt.util.EncodeDecodeUtil;
import com.apple.wwrc.foundation.configuration.impl.ConfigurationManagerImpl;
import com.apple.wwrc.foundation.db.manager.ConnectionManager;
import com.apple.wwrc.foundation.framework.identify.exception.AuthorizationException;
import com.apple.wwrc.foundation.framework.ssl.SSLFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.PrintWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServiceResolverTest {
  private static ServiceResolver resolver;

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
    SSLFactory.getInstance();
    ConnectionManager.init();
    resolver = new ServiceResolver()
      .setDelayStartSecond(0)
      .setPullingIntervalMinute(100)
      .build();
    //Await until resolver is ready to serve
    while (!resolver.ready()) {
      Thread.sleep(1 * 1000);
    }
  }

  @AfterAll
  public static void tearDownAfterTest() {
    resolver.tearDown();
    ConfigurationManagerImpl.tearDown();
    ConnectionManager.resetPool();
    new File("src/test/resources/test_ssl_properties").deleteOnExit();
  }

  @Test
  void testGetSourceDomain() throws Exception {
    assertEquals("PLATFORM", resolver.getSourceDomain("identity:idms.group.1232170"));
    assertEquals("RETAIL", resolver.getSourceDomain("identity:idms.group.1232161"));
    assertEquals("LEGACY", resolver.getSourceDomain("identity:idms.group.1232159"));
    assertEquals("", resolver.getSourceDomain("113"));
    assertThrows(AuthorizationException.class, () -> resolver.getSourceDomain("NOT_EXISTED"));
    assertThrows(AuthorizationException.class, () -> resolver.getSourceDomain(""));
    assertThrows(AuthorizationException.class, () -> resolver.getSourceDomain(null));
  }

  @Test
  void testGetServiceDomain() throws Exception {
    assertEquals("SECURITY", resolver.getTargetDomain("Authentication/v1.0", false));
    assertEquals("RETAIL", resolver.getTargetDomain("Bag/v1.0", false));
    assertEquals("LEGACY", resolver.getTargetDomain("LegacyBridge/v1.0", false));
    //Ignore version
    assertEquals("SECURITY", resolver.getTargetDomain("Authentication/v100.0", true));
    assertEquals("RETAIL", resolver.getTargetDomain("Bag/v100", true));
    assertEquals("LEGACY", resolver.getTargetDomain("LegacyBridge/v1.0", true));
    assertThrows(AuthorizationException.class, () -> resolver.getSourceDomain("NOT_EXISTED"));
    assertThrows(AuthorizationException.class, () -> resolver.getSourceDomain(""));
    assertThrows(AuthorizationException.class, () -> resolver.getSourceDomain(null));
  }

  @Test
  void testGetDirectPermission() throws Exception {
    assertEquals(true, resolver.validAccess("identity:idms.group.1232161", "Configuration/v1.0", "DOMAIN:PLATFORM"));
    assertEquals(true, resolver.validAccess("identity:idms.group.1232161", "Bag/v1.0", "any_method"));
    assertEquals(true, resolver.validAccess("identity:idms.group.1232161", "Authorization/v1.0", "isJwtAuthorized"));
    assertThrows(AuthorizationException.class, () -> resolver.validAccess(null, "Configuration/v1.0", "isJwtAuthorized"));
    assertThrows(AuthorizationException.class, () -> resolver.validAccess("identity:idms.group.1232161", null, "isJwtAuthorized"));
    assertThrows(AuthorizationException.class, () -> resolver.validAccess("identity:idms.group.1232161", "Configuration/v1.0", null));
    assertThrows(AuthorizationException.class, () -> resolver.validAccess("", "Configuration/v1.0", "isJwtAuthorized"));
    assertThrows(AuthorizationException.class, () -> resolver.validAccess("identity:idms.group.1232161", "", "isJwtAuthorized"));
    assertThrows(AuthorizationException.class, () -> resolver.validAccess("identity:idms.group.1232161", "Configuration/v1.0", ""));
  }

  @Test
  void testPermitAccessToPlatformService() throws Exception {
    assertEquals(true, resolver.validAccess("identity:idms.group.1232161", "Configuration/v1.0", "getConfigurationRows"));
    assertEquals(true, resolver.validAccess("1034", "ApiGateway/v1.0", "listServices"));
    assertThrows(AuthorizationException.class, () -> resolver.validAccess("NOT_EXISTED", "ApiGateway/v1.0", "listServices"));
    assertThrows(AuthorizationException.class, () -> resolver.validAccess("1034", "NOT_EXISTED", "unknownApi"));
  }

  @Test
  void testSameDomainAccess() throws Exception {
    assertEquals(true, resolver.validAccess("identity:idms.group.1232171", "ApiGateway/v1.0", "listServices"));
    assertThrows(AuthorizationException.class, () -> resolver.validAccess("NOT_EXISTED", "NOT_EXISTED/v1.0", "listServices"));
  }

  @Test
  void testCrossDomainAccess() throws Exception {
    assertEquals(true, resolver.validAccess("identity:idms.group.1232161", "LegacyBridgeUtil/v1.0", "createTransactionId"));
    assertEquals(false, resolver.validAccess("identity:idms.group.1232161", "JamaicaSecurity/v1.0", "piiEncypt"));
  }

  @Test
  void testHasPermissionIgnoreVersion() throws Exception {
    assertEquals(true, resolver.hasPermission("identity:idms.group.1232159", "WebOrderManagement/v1.0", "getWebOrderTransaction", false));
    assertEquals(false, resolver.hasPermission("identity:idms.group.1232159", "WebOrderManagement/v3.0", "getWebOrderTransaction", false));
    assertEquals(true, resolver.hasPermission("identity:idms.group.1232159", "WebOrderManagement/v3.0", "getWebOrderTransaction", true));
  }
}
