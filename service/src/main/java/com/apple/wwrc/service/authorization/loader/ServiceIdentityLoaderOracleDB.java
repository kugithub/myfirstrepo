package com.apple.wwrc.service.authorization.loader;

import com.apple.wwrc.service.authorization.datasource.ServiceIdentityRow;

import java.util.Map;

public class ServiceIdentityLoaderOracleDB extends AbstractPolicyLoader<ServiceIdentityRow> {
  public ServiceIdentityLoaderOracleDB() {
    super();
    countQuery = "SELECT Count(*) FROM SEC_SERVICE_IDENTITY";
    loadQuery = "SELECT SOURCE_ID, SOURCE_TYPE, SERVICE_NAME, DOMAIN_NAME "
      + "FROM SEC_SERVICE_IDENTITY "
      + "ORDER BY SOURCE_ID "
      + "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
    snapshotQuery = "SELECT MAX(ORA_ROWSCN) as SNAPSHOT from SEC_SERVICE_IDENTITY";
  }

  @Override
  protected String dataSource() {
    return "SEC_SERVICE_IDENTITY";
  }

  @Override
  @SuppressWarnings("rawtypes")
  protected ServiceIdentityRow resultSetToRow(Map map) {
    String sourceId = (String) map.get("SOURCE_ID");
    String sourceType = (String) map.get("SOURCE_TYPE");
    String serviceName = (String) map.get("SERVICE_NAME");
    String domainName = (String) map.get("DOMAIN_NAME");
    return new ServiceIdentityRow(sourceId, sourceType, serviceName, domainName);
  }
}
