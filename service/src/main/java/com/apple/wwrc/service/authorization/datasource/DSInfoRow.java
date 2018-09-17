package com.apple.wwrc.service.authorization.datasource;

import org.apache.commons.lang.builder.HashCodeBuilder;

import java.io.Serializable;

public class DSInfoRow implements Serializable {
  private static final long serialVersionUID = -2797158107414629893L;
  private String prsId = "PRS_ID";
  private String role = "ID_GP_WRK";
  private String salesOrg = "ID_SLS_ORG";
  private String storeId = "ID_STR_RT";
  public static final DSInfoRow UNCHAGED = new DSInfoRow("UNCHANGED", "", "", "");

  public DSInfoRow(String prsId,
    String role, String salesOrg, String storeId) {
    this.prsId = prsId;
    this.role = role;
    this.salesOrg = salesOrg;
    this.storeId = storeId;
  }

  @Override
  public boolean equals(Object o) {
    boolean isEqual = true;
    if (o != null && o.getClass() == this.getClass()) {
      DSInfoRow row = (DSInfoRow) o;
      isEqual = row.hashCode() == this.hashCode();
    } else {
      isEqual = false;
    }
    return isEqual;
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 31)
      .append(prsId)
      .append(role)
      .append(salesOrg)
      .append(storeId)
      .toHashCode();
  }

  public String getPrsId() {
    return prsId;
  }

  public String getRole() {
    return role;
  }

  public String getSalesOrg() {
    return salesOrg;
  }

  public String getStoreId() {
    return storeId;
  }
}
