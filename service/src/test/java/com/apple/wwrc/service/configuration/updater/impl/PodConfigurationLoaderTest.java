package com.apple.wwrc.service.configuration.updater.impl;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.apple.ist.retail.pos.encrypt.util.EncodeDecodeUtil;
import com.apple.wwrc.foundation.configuration.repository.ConfigurationRow;
import com.apple.wwrc.foundation.db.manager.ConnectionManager;
import com.apple.wwrc.service.configuration.util.ServiceFactoryProxy;

class PodConfigurationLoaderTest {
    @BeforeAll
    public static void setupBeforeAll() throws Exception {
        //oracle
        System.setProperty("com.apple.wwrc.db.jdbcUrl","jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=tcp)(HOST=falkar.corp.apple.com)(PORT=1874))(CONNECT_DATA=(SERVICE_NAME=nexus5d)))");
        System.setProperty("com.apple.wwrc.db.user", "NEXUS_POD_USER");
        System.setProperty("com.apple.wwrc.db.password",  EncodeDecodeUtil.encode("NEXUS_POD_USER_1234"));
        System.setProperty("com.apple.wwrc.db.type", "ORACLE");
        System.setProperty("id_groups", "NO_PULL");
    }
    @AfterEach
    void unstubbAfterEach() {
        ServiceFactoryProxy.unstubb();
    }
    @AfterAll
    public static void tearDownAfterTest() {
        try {
            ConnectionManager.resetPool();
        } catch (Exception e) {}
    }
    @Test
    void testLoadParameterTwiceDoesNotGetChange() {
        //Given
        PodConfigurationLoader loader = new PodConfigurationLoader();
        List<ConfigurationRow> rows = loader.load(Collections.emptySet());
        assertTrue(rows.size() > 1);
        //When
        rows = loader.load(Collections.emptySet());
        assertTrue(rows.isEmpty());
    }
    @Test
    void testParallelLoadReturnEmptyList() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        //Override isloading to true 
        PodConfigurationLoader loader = new PodConfigurationLoader();
        Field variable = loader.getClass().getDeclaredField("isloading");
        variable.setAccessible(true);
        variable.setBoolean(loader, true);
        //When
        List<ConfigurationRow> rows = loader.load(Collections.emptySet());
        assertTrue(rows.isEmpty());
    }

}
