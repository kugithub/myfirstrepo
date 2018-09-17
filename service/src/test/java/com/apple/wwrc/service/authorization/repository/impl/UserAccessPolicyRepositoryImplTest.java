package com.apple.wwrc.service.authorization.repository.impl;

import com.apple.wwrc.service.authorization.ServiceFactoryProxy;
import com.apple.wwrc.service.authorization.datasource.UserAccessPolicyRow;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserAccessPolicyRepositoryImplTest {

  private static final int SALES = -83451;
  private static final int STORE_MANAGER = -56964;
  private static UserAccessPolicyRepositoryImpl repo;

  @BeforeAll
  public static void setupBeforeTest() {
    repo = ServiceFactoryProxy.getUserAccessPolicyRepository();
  }

  @AfterEach
  public void tearDownAfterTest() {
    repo.tearDown();
    ServiceFactoryProxy.unstubb();
  }

  @Test
  void testUpdateRepository() throws Exception {
    //Given
    List<UserAccessPolicyRow> rows = new ArrayList<UserAccessPolicyRow>();
    rows.add(new UserAccessPolicyRow(SALES, "US", "R089", "iPhone inventory", "r-x"));
    rows.add(new UserAccessPolicyRow(SALES, "US", "R099", "iPhone inventory", "rw-"));
    rows.add(new UserAccessPolicyRow(SALES, "CN", "T009", "iPhone inventory", "--x"));
    rows.add(new UserAccessPolicyRow(SALES, "CN", null, "iPhone inventory", "---"));
    rows.add(new UserAccessPolicyRow(SALES, "US", null, "iPhone inventory", "rw-"));
    rows.add(new UserAccessPolicyRow(SALES, null, null, "iPhone inventory", "r--"));
    rows.add(new UserAccessPolicyRow(SALES, null, "T009", "invalid policy", "r--"));
    int before = repo.getLoadCount();
    //When
    repo.updateRepo(rows);
    //Then
    assertEquals(before + 1, repo.getLoadCount());
    assertEquals("r-x", repo.getPermissionString("" + SALES, "US", "R089", "iPhone inventory"));
    assertEquals("rw-", repo.getPermissionString("" + SALES, "US", "R099", "iPhone inventory"));
    assertEquals("--x", repo.getPermissionString("" + SALES, "CN", "T009", "iPhone inventory"));
    assertEquals("---", repo.getPermissionString("" + SALES, "CN", null, "iPhone inventory"));
    assertEquals("rw-", repo.getPermissionString("" + SALES, "US", null, "iPhone inventory"));
    assertEquals("r--", repo.getPermissionString("" + SALES, null, null, "iPhone inventory"));
    assertEquals("---", repo.getPermissionString("" + STORE_MANAGER, null, null, "iPhone inventory"));
  }

  @Test
  void testPolicyInheritant() throws Exception {
    //Given
    List<UserAccessPolicyRow> rows = new ArrayList<UserAccessPolicyRow>();
    rows.add(new UserAccessPolicyRow(SALES, null, null, "Global permission", "r--"));
    rows.add(new UserAccessPolicyRow(SALES, "US", null, "US only permission", "-w-"));
    rows.add(new UserAccessPolicyRow(SALES, "US", "R001", "R001 permission", "--x"));
    repo.updateRepo(rows);
    //Then Global Sales only has Global Rule(s)
    assertEquals("r--", repo.getPermissionString("" + SALES, null, null, "Global permission"));
    assertEquals("---", repo.getPermissionString("" + SALES, null, null, "US only permission"));
    assertEquals("---", repo.getPermissionString("" + SALES, null, null, "R001 permission"));
    //Then US's Sales inherit rules from Global Policy
    assertEquals("r--", repo.getPermissionString("" + SALES, "US", null, "Global permission"));
    assertEquals("-w-", repo.getPermissionString("" + SALES, "US", null, "US only permission"));
    assertEquals("---", repo.getPermissionString("" + SALES, "US", null, "R001 permission"));
    //Then R001 Sales inherit rules from US and Global Policy
    assertEquals("r--", repo.getPermissionString("" + SALES, "US", "R001", "Global permission"));
    assertEquals("-w-", repo.getPermissionString("" + SALES, "US", "R001", "US only permission"));
    assertEquals("--x", repo.getPermissionString("" + SALES, "US", "R001", "R001 permission"));
    //Then R000 Sales does not has R001 rule
    assertEquals("r--", repo.getPermissionString("" + SALES, "US", "R000", "Global permission"));
    assertEquals("-w-", repo.getPermissionString("" + SALES, "US", "R000", "US only permission"));
    assertEquals("---", repo.getPermissionString("" + SALES, "US", "R000", "R001 permission"));
  }

  @Test
  void testPolicyOverriden() throws Exception {
    //Given
    List<UserAccessPolicyRow> rows = new ArrayList<UserAccessPolicyRow>();
    rows.add(new UserAccessPolicyRow(SALES, null, null, "iPhone inventory", "---"));
    rows.add(new UserAccessPolicyRow(SALES, "US", null, "iPhone inventory", "r--"));
    rows.add(new UserAccessPolicyRow(SALES, "US", "R089", "iPhone inventory", "r-x"));
    repo.updateRepo(rows);
    //Then Global level get ---
    assertEquals("---", repo.getPermissionString("" + SALES, null, null, "iPhone inventory"));
    //Then US level get r--
    assertEquals("r--", repo.getPermissionString("" + SALES, "US", null, "iPhone inventory"));
    //Then Store R089 gets r-x
    assertEquals("r-x", repo.getPermissionString("" + SALES, "US", "R089", "iPhone inventory"));
    //Then NOT R089 get r-- (US default)
    assertEquals("r--", repo.getPermissionString("" + SALES, "US", "R099", "iPhone inventory"));
    //Then Other than US get --- (Global default)
    assertEquals("---", repo.getPermissionString("" + SALES, "CN", "T009", "iPhone inventory"));
    //Then NOT SALES get NULL
    assertEquals("---", repo.getPermissionString("" + STORE_MANAGER, null, null, "iPhone inventory"));
  }
}
