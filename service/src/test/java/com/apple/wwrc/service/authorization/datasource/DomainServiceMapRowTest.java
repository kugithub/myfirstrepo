package com.apple.wwrc.service.authorization.datasource;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DomainServiceMapRowTest {

  @Test
  void test() {
    assertEquals(DomainServiceMapRow.UNCHANGED, new DomainServiceMapRow());
    DomainServiceMapRow row = new DomainServiceMapRow("PLATFORM", "Configuration/v1.0");
    assertEquals("DomainServiceMap[domain=PLATFORM, service=Configuration/v1.0]", row.toString());
    assertEquals("PLATFORM", row.getDomain());
    assertEquals("Configuration/v1.0", row.getServiceWithVersion());
  }

}
