package com.apple.wwrc.service.customer;

import com.apple.wwrc.foundation.framework.Framework;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;

public class Mockingclass {
	private static Logger logger = Framework.getLogger(Mockingclass.class);

	List<Integer> arrayList = new ArrayList();


	public int listSize() {
		arrayList.add(1);
		arrayList.add(2);
		return arrayList.size();
	}

	public static String getString() {
		return "String";
	}

	public static String privateStaticMethodTest(){
		logger.info("Inside Static method");
		String priv = callPrivateMethod();
		logger.info("***** priv ***** " + priv);
		return priv;
	}

	private static String callPrivateMethod() {
		logger.info("Inside private method");
		return "private method";
//		throw new RuntimeException();
	}
}
