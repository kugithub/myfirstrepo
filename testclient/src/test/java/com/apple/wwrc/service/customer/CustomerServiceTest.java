package com.apple.wwrc.service.customer;

import com.apple.wwrc.foundation.framework.Framework;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit test for simple App.
 */
public class CustomerServiceTest {
	Logger logger = Framework.getLogger(CustomerServiceTest.class);

	@BeforeAll
	public static void setup() throws Exception {
        try (PrintWriter writer = new PrintWriter("src/test/resources/test_ssl_properties")) {
            writer.println("wwrc.pos.secure=false");
            writer.flush();
        }

        Properties env = System.getProperties();
        env.setProperty("bootStrapIp", "ws://localhost:9000/rcm");
        env.setProperty("wwrc.pos.ssl.config", "src/test/resources/test_ssl_properties");
	}

	@AfterAll
	public static void tearDown() {
        new File("src/test/resources/test_ssl_properties").deleteOnExit();
	}
	@Test
	@Disabled()
	public void dummyTest() {}

}