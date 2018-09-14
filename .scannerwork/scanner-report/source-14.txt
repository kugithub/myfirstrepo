package com.apple.wwrc.service.customer.util;

import java.util.HashMap;
import java.util.Map;

import com.apple.ist.ds2.pub.person.PersonServiceI;
import com.apple.ist.rpc2.RPCFactory;
import com.apple.wwrc.foundation.framework.service.ServiceFactory;
import com.apple.wwrc.foundation.security.service.SecurityInterface;

/**
 * A ServiceFactory that can be stubbed with mock client. It is a JUnit Friendly Factory.
 *
 * @author npoolsappasit
 */
public class ServiceFactoryProxy {
    private static Map<Class<?>, Object> repository = new HashMap<>();

    private ServiceFactoryProxy() {
        // prevent instantiation of the class
        throw new IllegalStateException("Constants is not instantiatable.");
    }
    /**
     * JUnit Only: You can stubb your mocked class here.
     */
    public static <T> void stubb(Class<T> clazz, T instance) {
        repository.put(clazz, instance);
    }

    public static SecurityInterface createJamaicaSecurityClient() {
        Object jamaicaSecurity = null;
        if (repository.containsKey(SecurityInterface.class)) {
            jamaicaSecurity = repository.get(SecurityInterface.class);
        }
        if (jamaicaSecurity == null || !SecurityInterface.class.isAssignableFrom(jamaicaSecurity.getClass())) {
            jamaicaSecurity = ServiceFactory.createClient(SecurityInterface.class);
        }
        return (SecurityInterface) jamaicaSecurity;
    }

    public static PersonServiceI createPersonServiceI() {
        Object service = null;
        if (repository.containsKey(PersonServiceI.class)) {
            service = repository.get(PersonServiceI.class);
        }
        if (service == null || !PersonServiceI.class.isAssignableFrom(service.getClass())) {
            service = RPCFactory.getService(PersonServiceI.class.getName());
        }
        return (PersonServiceI) service;
    }

    /**
     * JUnit Only:unstubb all the mocked classes on @AfterEach or @AfterAll
     */
    public static void unstubb() {
        repository.clear();
    }
}
