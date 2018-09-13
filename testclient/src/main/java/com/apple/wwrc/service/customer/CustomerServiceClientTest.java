package com.apple.wwrc.service.customer;

import com.apple.wwrc.foundation.framework.Framework;

import java.util.UUID;

import org.apache.logging.log4j.LogManager;

public class CustomerServiceClientTest {

	public static void main(String[] args) throws Exception {
		try {
			String uuid = UUID.randomUUID().toString();
			System.out.println("Calling to service uuid: " + uuid);
			CustomerServiceInterface service = Framework.createClient(CustomerServiceInterface.class);
		} finally {
			//Only use this method if you no longer need the connection to this endpoint.
			Framework.closeClient(CustomerServiceInterface.class);
		}
	}

}
