package com.apple.wwrc.service.customer;

import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class ConstantsTest {
    @Test(expected = InvocationTargetException.class)
    public void testInstantiablity()
            throws IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchMethodException {
            Constructor<Constants> constructor = Constants.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance(new Object[0]);
    }
}
