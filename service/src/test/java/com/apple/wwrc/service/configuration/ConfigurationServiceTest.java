package com.apple.wwrc.service.configuration;

import static org.junit.jupiter.api.Assertions.*;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.doNothing;
//add a comment here

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Matchers;

import com.apple.ist.retail.pos.encrypt.util.EncodeDecodeUtil;
import com.apple.wwrc.foundation.configuration.Constants;
import com.apple.wwrc.foundation.configuration.impl.ConfigurationContext;
import com.apple.wwrc.foundation.configuration.repository.AbstractConfigurationRepository;
import com.apple.wwrc.foundation.configuration.repository.ConfigurationRow;
import com.apple.wwrc.foundation.db.manager.ConnectionManager;
import com.apple.wwrc.service.configuration.util.ServiceFactoryProxy;

class ConfigurationServiceTest {
    private static ConfigurationService service;

    @BeforeAll
    public static void setupBeforeAll() throws Exception {
        //oracle
        System.setProperty("com.apple.wwrc.db.jdbcUrl","jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=tcp)(HOST=falkar.corp.apple.com)(PORT=1874))(CONNECT_DATA=(SERVICE_NAME=nexus5d)))");
        System.setProperty("com.apple.wwrc.db.user", "NEXUS_POD_USER");
        System.setProperty("com.apple.wwrc.db.password",  EncodeDecodeUtil.encode("NEXUS_POD_USER_1234"));
        System.setProperty("com.apple.wwrc.db.type", "ORACLE");
        System.setProperty("id_groups", "NO_PULL");
        service = new ConfigurationService();
        assertEquals(false, service.healthCheck());
        service.bootstrap();
        assertEquals(true, service.healthCheck());
    }
    @AfterEach
    void unstubbAfterEach() {
        ServiceFactoryProxy.unstubb();
    }
    @AfterAll
    public static void tearDownAfterTest() {
        try {
            service.tearDown();
            assertEquals(false, service.healthCheck());
            ConnectionManager.resetPool();
        } catch (Exception e) {}
    }

    @Test
    void test() throws Exception {
        //When: Load for the first time
        List<ConfigurationRow> configs = service.getConfigurationRows("iphone.service.pod", "checksum", 0);
        int expectedSize = configs.size();

        //When: Load for a second time does not change size.
        configs = service.getConfigurationRows("iphone.service.pod", "checksum", 0);
        assertEquals(expectedSize, configs.size());

        //When: Begin page > End Index return empty list
        configs = service.getConfigurationRows("iphone.service.pod", "checksum", 3000);
        assertEquals(1, configs.size());
        assertEquals(true, ConfigurationRow.isParityRow(configs.get(0)));

        //When: Load with up-to-date checksum returns ConfigurationRow.UNCHAGED
        String updated_checksum = configs.get(configs.size()-1).getValue();//This is a parity element
        configs = service.getConfigurationRows("iphone.service.pod", updated_checksum, 0);
        assertEquals(1, configs.size());
        assertEquals(ConfigurationRow.UNCHAGED, configs.get(0));
        
        //When: NS Not Found
        configs = service.getConfigurationRows("this-freaking-namespace-doesnot-exist", "checksum", 0);
        assertEquals(1, configs.size());
        assertEquals(ConfigurationRow.NOTFOUND, configs.get(0));
    }

    @Test
    void testLoadStoreToSaleOrg() throws Exception {
        //Given
        ConfigurationContext cntxt = new ConfigurationContext(Constants.SALES_ORG_STOREID_MAP_NAMESPACE);
        //Then
        List<ConfigurationRow> configs = service.getConfigurationRows(Constants.SALES_ORG_STOREID_MAP_NAMESPACE, "checksum", 0);
        assertEquals(Constants.PAGINATION_SIZE + 1, configs.size());//plus one parity row
        //When Data has not changed
        String checksum = configs.get(configs.size()-1).getValue();//This is a parity element
        configs = service.getConfigurationRows(cntxt.getNamespace(), checksum, 0);
        //Then
        assertEquals(1, configs.size());
        assertEquals(ConfigurationRow.UNCHAGED, configs.get(0));
    }

    @Test
    void testLoadDataCenterInfo() throws Exception {
        ConfigurationContext cntxt = new ConfigurationContext("data.center.default.map", "5343", "R005");
        //Given + When
        List<ConfigurationRow> configs = service.getConfigurationRows("data.center.default.map", "checksum", 0);
        //Then
        assertEquals(1 + 1, configs.size());//plus one parity row
        String datacenterValue = configs.get(0).getValue();
        String checksumValue = configs.get(1).getValue();//This is a parity element
        assertEquals("100", datacenterValue);
        assertFalse("checksum".equals(checksumValue));
        //When: load for the second time.
        configs = service.getConfigurationRows("data.center.default.map", checksumValue, 0);
        //Then
        assertEquals(1, configs.size());
        assertEquals(ConfigurationRow.UNCHAGED, configs.get(0));
    }

    @Test
    void testAbnomalies() throws Exception {
        //Given
        AbstractConfigurationRepository mRepo = mock(AbstractConfigurationRepository.class);
        when(mRepo.bootstrap()).thenReturn(new HashSet<String>(Arrays.asList("this-namespace-throw-NPE")));
        when(mRepo.isReady()).thenReturn(true);
        doNothing().when(mRepo).updateRepo(Matchers.any());
        ServiceFactoryProxy.stubb(AbstractConfigurationRepository.class, mRepo);
        when(mRepo.getConfigurationRows("this-namespace-throw-NPE", "checksum")).thenThrow(NullPointerException.class);
        //When
        ConfigurationService service_ = new ConfigurationService();
        service_.bootstrap();
        List<ConfigurationRow> configs = service_.getConfigurationRows("this-namespace-throw-NPE", "checksum", 0);
        //Then
        assertEquals(0, configs.size());
        //Clean up
        ServiceFactoryProxy.unstubb();
    }
}
