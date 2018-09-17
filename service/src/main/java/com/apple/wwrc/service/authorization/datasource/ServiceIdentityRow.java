package com.apple.wwrc.service.authorization.datasource;

import org.apache.commons.lang.builder.HashCodeBuilder;

import java.io.Serializable;

@SuppressWarnings({"squid:S00103", "common-java:InsufficientCommentDensity", "squid:S1166", "squid:S109"})
public class ServiceIdentityRow implements Serializable {
  private static final long serialVersionUID = 1874052117700463458L;
  private String sourceId = "";
  private String sourceType = "";
  private String serviceName = "";
  private String domainName = "";
  public static final ServiceIdentityRow UNCHANGED = new ServiceIdentityRow();

  public ServiceIdentityRow() {
  }

  public ServiceIdentityRow(String sourceId, String sourceType, String serviceName, String domainName) {
    this.sourceId = sourceId;
    this.sourceType = sourceType;
    this.serviceName = serviceName;
    this.domainName = domainName;
  }

  public String getSourceId() {
    return sourceId;
  }

  public String getSourceType() {
    return sourceType;
  }

  public String getServiceName() {
    return serviceName;
  }

  public String getDomainName() {
    return domainName;
  }

  @Override
  public String toString() {
    return String.format("ServiceIdentity[src=%s, typ=%s, name=%s, domain=%s]", sourceId, sourceType, serviceName, domainName);
  }

  @Override
  public boolean equals(Object o) {
    boolean isEqual = true;
    if (o != null && o.getClass() == this.getClass()) {
      ServiceIdentityRow row = (ServiceIdentityRow) o;
      isEqual = row.hashCode() == this.hashCode();
    } else {
      isEqual = false;
    }
    return isEqual;
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 31)
      .append(sourceId)
      .toHashCode();
  }
}
