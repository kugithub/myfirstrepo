package com.apple.wwrc.service.authorization.datasource;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DSInfoRowTest {
  @Test
  void test() {
    DSInfoRow row = DSInfoRow.UNCHAGED;
    assertTrue(row.equals(new DSInfoRow("UNCHANGED", "", "", "")));
    assertEquals(row.hashCode(), new DSInfoRow("UNCHANGED", "", "", "").hashCode());
  }

  @Test
  void testDSInfoRow() {
    DSInfoRow row = new DSInfoRow("Id", "role", "salesOrg", "storeId");
    assertEquals(row, new DSInfoRow("Id", "role", "salesOrg", "storeId"));
    assertEquals("Id", row.getPrsId());
    assertEquals("role", row.getRole());
    assertEquals("salesOrg", row.getSalesOrg());
    assertEquals("storeId", row.getStoreId());
  }
}
