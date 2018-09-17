package com.apple.wwrc.service.authorization.datasource;

import org.apache.commons.lang.builder.HashCodeBuilder;

import java.io.Serializable;

public class DomainServiceMapRow implements Serializable {
  private static final long serialVersionUID = 5166259719954149400L;
  public static final DomainServiceMapRow UNCHANGED = new DomainServiceMapRow();
  private String domain = "";
  private String serviceWithVersion = "";

  public DomainServiceMapRow() {
  }

  public DomainServiceMapRow(String domain, String versionedService) {
    this.domain = domain;
    this.serviceWithVersion = versionedService;
  }

  public String getDomain() {
    return domain;
  }

  public String getServiceWithVersion() {
    return serviceWithVersion;
  }

  @Override
  public String toString() {
    return String.format("DomainServiceMap[domain=%s, service=%s]", domain, serviceWithVersion);
  }

  @Override
  public boolean equals(Object o) {
    boolean isEqual = true;
    if (o != null && o.getClass() == this.getClass()) {
      DomainServiceMapRow row = (DomainServiceMapRow) o;
      isEqual = row.hashCode() == this.hashCode();
    } else {
      isEqual = false;
    }
    return isEqual;
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 31)
      .append(domain)
      .append(serviceWithVersion)
      .toHashCode();
  }
}
