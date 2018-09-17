package com.apple.wwrc.service.authorization.loader;

import com.apple.wwrc.service.authorization.datasource.UserAccessPolicyRow;

import java.util.Map;

public class UserAccessPolicyLoader extends AbstractPolicyLoader<UserAccessPolicyRow> {
  public UserAccessPolicyLoader() {
    super();
    countQuery = "SELECT Count(*) FROM CO_ACS_GP_RS";
    loadQuery = "SELECT ID_GP_WRK as ROLE, ID_SLS_ORG as SALEORG, "
      + "DE_RS as PERMISSION, FL_ACS_GP_RD as R, FL_ACS_GP_WR as W "
      + "FROM CO_ACS_GP_RS "
      + "ORDER BY ID_RS "
      + "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
    snapshotQuery = "SELECT MAX(ORA_ROWSCN) as SNAPSHOT from CO_ACS_GP_RS";
  }

  protected UserAccessPolicyRow resultSetToRow(Map map) {
    int roleId = Integer.valueOf((String) map.get("ROLE"));
    String saleOrg = map.get("SALEORG") == null ? "ALL" : String.valueOf(map.get("SALEORG"));
    String storeId = "ALL";//For now
    String key = String.valueOf(map.get("PERMISSION"));
    String read = String.valueOf(map.get("R")).equals("0") ? "-" : "r";
    String write = String.valueOf(map.get("W")).equals("0") ? "-" : "w";
    String execute = "-";//For now
    return new UserAccessPolicyRow(roleId, saleOrg, storeId, key, read + write + execute);
  }

  @Override
  protected String dataSource() {
    return "CO_ACS_GP_RS";
  }
}
