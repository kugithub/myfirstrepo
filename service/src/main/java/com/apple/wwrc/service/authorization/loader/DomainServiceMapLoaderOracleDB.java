package com.apple.wwrc.service.authorization.loader;

import com.apple.wwrc.service.authorization.datasource.DomainServiceMapRow;

import java.util.Map;

public class DomainServiceMapLoaderOracleDB extends AbstractPolicyLoader<DomainServiceMapRow> {
  public DomainServiceMapLoaderOracleDB() {
    super();
    countQuery = "SELECT Count(*) FROM SEC_DOMAIN_SERVICE_MAP";
    loadQuery = "SELECT DOMAIN_NAME, SERVICE_NAME "
      + "FROM SEC_DOMAIN_SERVICE_MAP "
      + "ORDER BY SERVICE_NAME "
      + "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
    snapshotQuery = "SELECT MAX(ORA_ROWSCN) as SNAPSHOT from SEC_DOMAIN_SERVICE_MAP";
  }

  @Override
  protected String dataSource() {
    return "SEC_DOMAIN_SERVICE_MAP";
  }

  @Override
  @SuppressWarnings("rawtypes")
  protected DomainServiceMapRow resultSetToRow(Map map) {
    String domainName = (String) map.get("DOMAIN_NAME");
    String serviceWithVersion = (String) map.get("SERVICE_NAME");
    return new DomainServiceMapRow(domainName, serviceWithVersion);
  }
}
