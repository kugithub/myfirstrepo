package com.apple.wwrc.service.configuration.util;

import java.util.HashMap;
import java.util.Map;

import com.apple.wwrc.foundation.configuration.repository.AbstractConfigurationRepository;
import com.apple.wwrc.service.configuration.datasource.MasterConfigurationRepositoryImpl;

/**
 * ServiceFactoryProxy is JUnit friendly factory allowing you to mock all its products.
 * @author npoolsappasit
 */
public class ServiceFactoryProxy {
    private ServiceFactoryProxy() { }
    @SuppressWarnings("rawtypes")
    private static Map repository = new HashMap<Class<?>, Object>();

    @SuppressWarnings("unchecked")
    /**
     * JUnit Only: You can stubb your mocked class here.
     */
    public static <T> void stubb(Class<T> clazz, T instance) {
        repository.put(clazz, instance);
    }
    public static synchronized AbstractConfigurationRepository configurationRepository() {
        Object repo = null;
        if (repository.containsKey(AbstractConfigurationRepository.class)) {
            repo = repository.get(AbstractConfigurationRepository.class);
        }
        if (repo == null || !AbstractConfigurationRepository.class.isAssignableFrom(repo.getClass())) {
            repo = MasterConfigurationRepositoryImpl.getInstance();
            stubb(AbstractConfigurationRepository.class, (AbstractConfigurationRepository) repo);
        }
        return (AbstractConfigurationRepository) repo;
    }
    /**
     * JUnit Only:unstubb all the mocked classes on @AfterEach or @AfterAll 
     */
    public static void unstubb() {
        repository.clear();
    }
}
