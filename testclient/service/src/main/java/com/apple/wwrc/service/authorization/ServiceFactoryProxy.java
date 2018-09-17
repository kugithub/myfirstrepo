package com.apple.wwrc.service.authorization;

import com.apple.wwrc.foundation.framework.exception.FrameworkException;
import com.apple.wwrc.foundation.framework.ssl.SSLFactory;
import com.apple.wwrc.service.authorization.repository.impl.UserAccessPolicyRepositoryImpl;

import java.util.HashMap;
import java.util.Map;

/**
 * A ServiceFactory that can be stubbed with mock client. It is a JUnit Friendly Factory.
 * @author npoolsappasit
 */
public class ServiceFactoryProxy {
  private ServiceFactoryProxy() {
  }

  @SuppressWarnings("rawtypes")
  private static Map repository = new HashMap<Class<?>, Object>();

  @SuppressWarnings("unchecked")
  /**
   * JUnit Only: You can stubb your mocked class here.
   */
  public static <T> void stubb(Class<T> clazz, T instance) {
    repository.put(clazz, instance);
  }

  public static synchronized SSLFactory sslFactory() throws FrameworkException {
    Object ssl = null;
    if (repository.containsKey(SSLFactory.class)) {
      ssl = repository.get(SSLFactory.class);
    }
    if (ssl == null || !SSLFactory.class.isAssignableFrom(ssl.getClass())) {
      ssl = SSLFactory.getInstance();
      stubb(SSLFactory.class, (SSLFactory) ssl);
    }
    return (SSLFactory) ssl;
  }

  public static UserAccessPolicyRepositoryImpl getUserAccessPolicyRepository() {
    Object repo = null;
    if (repository.containsKey(UserAccessPolicyRepositoryImpl.class)) {
      repo = repository.get(UserAccessPolicyRepositoryImpl.class);
    }
    if (repo == null || !UserAccessPolicyRepositoryImpl.class.isAssignableFrom(repo.getClass())) {
      repo = new UserAccessPolicyRepositoryImpl();
      stubb(UserAccessPolicyRepositoryImpl.class, (UserAccessPolicyRepositoryImpl) repo);
    }
    return (UserAccessPolicyRepositoryImpl) repo;
  }

  /**
   * JUnit Only:unstubb all the mocked classes on @AfterEach or @AfterAll
   */
  public static void unstubb() {
    repository.clear();
  }
}
