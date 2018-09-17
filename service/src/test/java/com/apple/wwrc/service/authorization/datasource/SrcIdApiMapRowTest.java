package com.apple.wwrc.service.authorization.datasource;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SrcIdApiMapRowTest {

  @Test
  void test() {
    assertEquals(SrcIdApiMapRow.UNCHANGED, new SrcIdApiMapRow());
    SrcIdApiMapRow row = new SrcIdApiMapRow("sourceId", "targetService", "apiName");
    assertEquals("X-SrvPermission[src=sourceId,target=targetService,api=apiName]", row.toString());
  }

}
