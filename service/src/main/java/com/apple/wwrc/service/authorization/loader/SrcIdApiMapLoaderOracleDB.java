package com.apple.wwrc.service.authorization.loader;

import com.apple.wwrc.service.authorization.datasource.SrcIdApiMapRow;

import java.util.Map;

public class SrcIdApiMapLoaderOracleDB extends AbstractPolicyLoader<SrcIdApiMapRow> {
  public SrcIdApiMapLoaderOracleDB() {
    super();
    countQuery = "SELECT Count(*) FROM SEC_SOURCEID_API_MAP";
    loadQuery = "SELECT SOURCE_ID, TARGET_SERVICE, API_NAME "
      + "FROM SEC_SOURCEID_API_MAP "
      + "ORDER BY SOURCE_ID "
      + "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
    snapshotQuery = "SELECT MAX(ORA_ROWSCN) as SNAPSHOT from SEC_SOURCEID_API_MAP";
  }

  @Override
  protected String dataSource() {
    return "SEC_SOURCEID_API_MAP";
  }

  @Override
  @SuppressWarnings("rawtypes")
  protected SrcIdApiMapRow resultSetToRow(Map map) {
    String sourceId = (String) map.get("SOURCE_ID");
    String targetService = (String) map.get("TARGET_SERVICE");
    String apiName = (String) map.get("API_NAME");
    return new SrcIdApiMapRow(sourceId, targetService, apiName);
  }
}
