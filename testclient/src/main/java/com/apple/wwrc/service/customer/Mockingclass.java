package com.apple.wwrc.service.customer;

import com.apple.wwrc.foundation.framework.Framework;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

public class Mockingclass {
    private static final Logger LOGGER = Framework.getLogger(Mockingclass.class);
    private static final String STRING_VALUE = "String";
    private List<Integer> arrayList = new ArrayList<>();


    public int listSize() {
        arrayList.add(1);
        arrayList.add(2);
        return arrayList.size();
    }

    public static String getString() {
        return STRING_VALUE;
    }

    public static String privateStaticMethodTest() {
        LOGGER.info("Inside Static method");
        String priv = callPrivateMethod();
        LOGGER.info("***** priv ***** {}", priv);
        return priv;
    }

    private static String callPrivateMethod() {
        LOGGER.info("Inside private method");
        return "private method";
//		throw new RuntimeException()
    }
}
