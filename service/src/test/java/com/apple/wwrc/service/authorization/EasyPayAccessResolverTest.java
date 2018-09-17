package com.apple.wwrc.service.authorization;

import com.apple.ist.retail.pos.encrypt.util.EncodeDecodeUtil;
import com.apple.wwrc.foundation.configuration.impl.ConfigurationManagerImpl;
import com.apple.wwrc.foundation.db.manager.ConnectionManager;
import com.apple.wwrc.foundation.framework.identify.exception.AuthorizationException;
import com.apple.wwrc.service.authorization.resolver.UserInfoResolver;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EasyPayAccessResolverTest {
  static UserInfoResolver resolver;

  @BeforeAll
  public static void setupBeforeAll() throws Exception {
    //oracle
    System.setProperty("com.apple.wwrc.db.jdbcUrl",
      "jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=tcp)(HOST=falkar.corp.apple.com)(PORT=1871))(CONNECT_DATA=(SERVICE_NAME=nexus2d)))");
    System.setProperty("com.apple.wwrc.db.user", "nexus_user");
    System.setProperty("com.apple.wwrc.db.password", EncodeDecodeUtil.encode("xyonnexususerpassword"));
    System.setProperty("com.apple.wwrc.db.type", "ORACLE");
    System.setProperty("RPC", "false");
    System.setProperty("id_groups", "POS");
    ConnectionManager.init();
    resolver = new UserInfoResolver();
  }

  @AfterAll
  public static void tearDownAfterTest() {
    try {
      resolver.tearDown();
      ConfigurationManagerImpl.tearDown();
      ConnectionManager.resetPool();
    } catch (Exception e) {
    }
  }

  @Test
  @Disabled()
  void dummy() {
  }

  @Test
  void testEasyPayStorePermission() throws AuthorizationException {
    //Given
    /* [DATA]
     *******************************
     *   P     *  55795 * nkguy_od *
     *   T     *  55795 * nkguy_od *
     *   R092  *  55795 * nkguy_od *
     *******************************
     */
    int USER = 55795;
    String STORE_ONE = "R092";
    String STORE_TWO = "R093";
    String STORE_THR = "R094";
    //Use Mockito spy to byPass configuration load which took long time.
    UserInfoResolver resolver_ = Mockito.spy(resolver);
    Mockito.when(resolver_.salesOrgFromStoreId(STORE_ONE)).thenReturn("1583");
    Mockito.when(resolver_.salesOrgFromStoreId(STORE_TWO)).thenReturn("1583");
    Mockito.when(resolver_.salesOrgFromStoreId(STORE_THR)).thenReturn("5630");
    //When+Then
    assertEquals(true, resolver_.hasEasyPayAccess(USER, STORE_ONE));
    assertEquals(false, resolver_.hasEasyPayAccess(USER, STORE_TWO));
    assertEquals(false, resolver_.hasEasyPayAccess(USER, STORE_THR));
  }

  @Test
  void testEasyPayGeoPermission() throws AuthorizationException {
    //No sample in DB that has GeoPermission to test
  }
}
