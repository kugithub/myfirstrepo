package com.apple.wwrc.service.authorization.datasource;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServiceIdentityRowTest {

  @Test
  void test() {
    assertEquals(ServiceIdentityRow.UNCHANGED, new ServiceIdentityRow());
    ServiceIdentityRow row1 = new ServiceIdentityRow("123456", "AppId", "ServiceName", "PLATFORM");
    assertEquals("123456", row1.getSourceId());
    assertEquals("AppId", row1.getSourceType());
    assertEquals("ServiceName", row1.getServiceName());
    assertEquals("PLATFORM", row1.getDomainName());
    ServiceIdentityRow row2 = new ServiceIdentityRow("123456", "AppId", "ServiceName", "PLATFORM");
    assertEquals(row1, row2);
    assertEquals("ServiceIdentity[src=123456, typ=AppId, name=ServiceName, domain=PLATFORM]", row1.toString());
  }

}
