package com.apple.wwrc.service.customer;

import com.apple.wwrc.foundation.framework.Framework;
import org.slf4j.Logger;

import java.util.UUID;

public class CustomerServiceClientTest {
    private static final Logger LOGGER = Framework.getLogger(CustomerServiceClientTest.class);

    public static void main(String[] args) {
        try {
            String uuid = UUID.randomUUID().toString();
            LOGGER.info("Calling to service uuid: {}", uuid);
            Framework.createClient(CustomerServiceInterface.class);
        } finally {
            //Only use this method if you no longer need the connection to this endpoint.
            Framework.closeClient(CustomerServiceInterface.class);
        }
    }

}
