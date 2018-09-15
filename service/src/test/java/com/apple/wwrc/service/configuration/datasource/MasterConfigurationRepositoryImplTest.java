package com.apple.wwrc.service.configuration.datasource;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import com.apple.wwrc.foundation.configuration.Constants;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.apple.wwrc.foundation.configuration.ConfigurationManager;
import com.apple.wwrc.foundation.configuration.exception.UnChangedException;
import com.apple.wwrc.foundation.configuration.impl.ConfigurationContext;
import com.apple.wwrc.foundation.configuration.repository.ConfigurationRow;
import com.apple.wwrc.service.configuration.util.ServiceFactoryProxy;

class MasterConfigurationRepositoryImplTest {
    private static ArrayList<ConfigurationRow> data = new ArrayList<ConfigurationRow>();
    @BeforeAll
    public static void setupBeforeClass() throws Exception {
        //Preload initial configurations
        ConfigurationRow r01 = new ConfigurationRow("default", "tender", "US", "R390", "101", "key1", "101-Override");
        ConfigurationRow r02 = new ConfigurationRow("default", "tender", "US", "R390", ""   , "key1", "R390-Override");
        ConfigurationRow r03 = new ConfigurationRow("default", "tender", "US", ""    , ""   , "key1", "US-Override");
        ConfigurationRow r04 = new ConfigurationRow("default", "tender", "CN", ""    , ""   , "key1", "CN-Override");
        ConfigurationRow r05 = new ConfigurationRow("default", "tender", ""  , ""    , ""   , "key1", "tender-default");
        ConfigurationRow r06 = new ConfigurationRow("default", "tender", ""  , ""    , ""   , "key2", "tender-only");
        ConfigurationRow r07 = new ConfigurationRow("default", "tender", "US", ""    , ""   , "key3", "US-only");
        ConfigurationRow r08 = new ConfigurationRow("default", "tender", "US", "R390", ""   , "key4", "R390-only");
        ConfigurationRow r09 = new ConfigurationRow("default", "tender", "US", "R390", "101", "key5", "101-only");

        //SalesOrg
        ConfigurationRow r10 = new ConfigurationRow("default", Constants.SALES_ORG_STOREID_MAP_NAMESPACE, "ALL", "ALL", "ALL", "Store-1", "US");
        ConfigurationRow r11 = new ConfigurationRow("default", Constants.SALES_ORG_STOREID_MAP_NAMESPACE, "ALL", "ALL", "ALL", "Store-2", "US");
        ConfigurationRow r12 = new ConfigurationRow("default", Constants.SALES_ORG_STOREID_MAP_NAMESPACE, "ALL", "ALL", "ALL", "Store-3", "US");

        //With Config_Set_Name
        ConfigurationRow r13 = new ConfigurationRow("Orbit01", "tender", ""  , ""    , ""   , "key1", "orbit-Override");
        ConfigurationRow r14 = new ConfigurationRow("Orbit01", "tender", "US", ""    , ""   , "key1", "orbit-Override-1");
        ConfigurationRow r15 = new ConfigurationRow("Orbit01", "tender", "US", "R390", ""   , "key1", "orbit-Override-2");
        ConfigurationRow r16 = new ConfigurationRow("Orbit01", "tender", "US", "R390", "101", "key1", "orbit-Override-3");
        //With other namespace
        ConfigurationRow r17 = new ConfigurationRow("default", "txn", "", "", "", "key1", "default-1");
        ConfigurationRow r18 = new ConfigurationRow("Orbit01", "txn", "", "", "", "key2", "orbit-2");
        data.add(r01);  data.add(r02);  data.add(r03);
        data.add(r04);  data.add(r05);  data.add(r06);
        data.add(r07);  data.add(r08);  data.add(r09);
        data.add(r10);  data.add(r11);  data.add(r12);
        data.add(r13);  data.add(r14);  data.add(r15);
        data.add(r16);  data.add(r17);  data.add(r18);
    }
    @AfterEach
    void unstubbAfterEach() {
        ServiceFactoryProxy.unstubb();
    }
    @Test
    void testLoadConfigurationRows() throws Exception {
        //Given
        MasterConfigurationRepositoryImpl repo = new MasterConfigurationRepositoryImpl();
        repo.updateRepo(data);
        //When
        ConfigurationContext tender = new ConfigurationContext("tender");
        ConfigurationContext txn = new ConfigurationContext("txn");
        //Then
        List<ConfigurationRow> tender_configs = repo.getConfigurationRows(tender.getNamespace(), "checksum");
        List<ConfigurationRow> txn_configs = repo.getConfigurationRows(txn.getNamespace(), "checksum");
        assertEquals(13, tender_configs.size());
        assertEquals(2, txn_configs.size());
    }
    @Test
    void testUpdateCacheOnParameterTableChangeNotOverrideSalesOrgCache() throws Exception {
        //Given
        MasterConfigurationRepositoryImpl repo = new MasterConfigurationRepositoryImpl();
        repo.updateRepo(data);
        ConfigurationContext salesOrg = new ConfigurationContext(Constants.SALES_ORG_STOREID_MAP_NAMESPACE);
        assertEquals(3, repo.getConfigurationRows(Constants.SALES_ORG_STOREID_MAP_NAMESPACE, "checksum").size());
        assertEquals(18, repo.getConfigurationRows(Constants.ALL_NAMESPACES, "checksum").size());
        //When
        ConfigurationRow r01 = new ConfigurationRow("default", "tender", "US", ""    , ""   , "key13", "US-only");
        ConfigurationRow r02 = new ConfigurationRow("default", "tender", "US", "R390", ""   , "key14", "R390-only");
        ConfigurationRow r03 = new ConfigurationRow("default", "tender", "US", "R390", "101", "key15", "101-only");
        ArrayList<ConfigurationRow> newdata = new ArrayList<ConfigurationRow>();
        newdata.add(r01);newdata.add(r02);newdata.add(r02);
        repo.updateRepo(newdata);
        //Then
        assertEquals(3, repo.getConfigurationRows(Constants.SALES_ORG_STOREID_MAP_NAMESPACE, "checksum").size());
        assertEquals(6, repo.getConfigurationRows(Constants.ALL_NAMESPACES, "checksum").size());
    }
    @Test
    void testUpdateCacheOnSalesOrgTableChangeNotOverrideParameterCache() throws Exception {
        //Given
        MasterConfigurationRepositoryImpl repo = new MasterConfigurationRepositoryImpl();
        repo.updateRepo(data);
        ConfigurationContext salesOrg = new ConfigurationContext(Constants.SALES_ORG_STOREID_MAP_NAMESPACE);
        assertEquals(13, repo.getConfigurationRows("tender", "checksum").size());
        assertEquals(18, repo.getConfigurationRows(Constants.ALL_NAMESPACES, "checksum").size());
        //When
        ConfigurationRow r01 = new ConfigurationRow("default", Constants.SALES_ORG_STOREID_MAP_NAMESPACE, "ALL", "ALL", "ALL", "Store-1", "CN");
        ConfigurationRow r02 = new ConfigurationRow("default", Constants.SALES_ORG_STOREID_MAP_NAMESPACE, "ALL", "ALL", "ALL", "Store-2", "CN");
        ConfigurationRow r03 = new ConfigurationRow("default", Constants.SALES_ORG_STOREID_MAP_NAMESPACE, "ALL", "ALL", "ALL", "Store-3", "CN");
        ArrayList<ConfigurationRow> newdata = new ArrayList<ConfigurationRow>();
        newdata.add(r01);newdata.add(r02);newdata.add(r02);
        repo.updateRepo(newdata);
        //Then
        assertEquals(13, repo.getConfigurationRows("tender", "checksum").size());
        assertEquals(18, repo.getConfigurationRows(Constants.ALL_NAMESPACES, "checksum").size());
    }
    @Test
    void testUnimplementedMethods() {
        //Given
        MasterConfigurationRepositoryImpl repo = new MasterConfigurationRepositoryImpl();
        assertEquals(0,repo.getConfigurationsForFileCache().size());
        assertEquals(0, repo.bootstrap().size());
        assertNull(repo.getConfigurations(new ConfigurationContext("POS")));
    }
}
