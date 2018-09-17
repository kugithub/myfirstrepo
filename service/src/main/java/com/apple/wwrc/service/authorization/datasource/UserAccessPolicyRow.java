package com.apple.wwrc.service.authorization.datasource;

import com.apple.wwrc.foundation.configuration.impl.ConfigurationContext;

import java.io.Serializable;

/**
 * The Class ConfigurationRow.
 */
public final class UserAccessPolicyRow implements Serializable {
  private static final long serialVersionUID = 3642912123600775447L;
  public static final UserAccessPolicyRow UNCHAGED = new UserAccessPolicyRow(-1, "ALL", "ALL", "ALL", "UNCHANGED");
  public static final UserAccessPolicyRow NOTFOUND = new UserAccessPolicyRow(-1, "ALL", "ALL", "ALL", "NOTFOUND");
  private static final String ALL = "ALL";

  private int roleId;
  private String configSetName;
  private String salesOrg;
  private String store;
  private String permissionKey;
  private String value;

  public UserAccessPolicyRow(int roleId, String salesOrg, String store, String key, String value) {
    this.configSetName = ConfigurationContext.DEFAULT_;
    this.roleId = roleId;
    this.salesOrg = (salesOrg == null ? ALL : salesOrg.trim());
    this.store = (store == null ? ALL : store.trim());
    ;
    this.permissionKey = key.trim();
    if (value != null) {
      this.value = value.trim();
    }
  }

  public int getRoleId() {
    return roleId;
  }

  public String getConfigSetName() {
    return configSetName;
  }

  public String getSalesOrg() {
    return salesOrg;
  }

  public String getStore() {
    return store;
  }

  public String getPermissionKey() {
    return permissionKey;
  }

  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return "AccessRule[roleId=" + roleId
      + ", salesOrg=" + salesOrg + ", store=" + store
      + ", key=" + permissionKey + ", value=" + value + ","
      + ", conf_set_name=" + configSetName + "]";
  }
}
