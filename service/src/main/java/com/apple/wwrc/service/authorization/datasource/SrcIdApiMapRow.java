package com.apple.wwrc.service.authorization.datasource;

import org.apache.commons.lang.builder.HashCodeBuilder;

import java.io.Serializable;

public class SrcIdApiMapRow implements Serializable {
  private static final long serialVersionUID = -7319521805000472521L;
  private String sourceId = "";
  private String targetService = "";
  private String apiName = "";
  public static final SrcIdApiMapRow UNCHANGED = new SrcIdApiMapRow();

  public SrcIdApiMapRow() {
  }

  public SrcIdApiMapRow(String sourceId, String targetService, String apiName) {
    this.sourceId = sourceId;
    this.targetService = targetService;
    this.apiName = apiName;
  }

  public String sourceId() {
    return sourceId;
  }

  public String targetService() {
    return targetService;
  }

  public String apiName() {
    return apiName;
  }

  @Override
  public String toString() {
    return String.format("X-SrvPermission[src=%s,target=%s,api=%s]", sourceId, targetService, apiName);
  }

  @Override
  public boolean equals(Object o) {
    boolean isEqual = true;
    if (o != null && o.getClass() == this.getClass()) {
      isEqual = o.hashCode() == this.hashCode();
    } else {
      isEqual = false;
    }
    return isEqual;
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 31)
      .append(sourceId)
      .append(targetService)
      .append(apiName)
      .toHashCode();
  }
}
