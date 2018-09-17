package com.apple.wwrc.service.authorization.scheduler;

import com.apple.wwrc.foundation.configuration.updater.AbstractLoader;
import com.apple.wwrc.foundation.configuration.updater.AbstractRepository;
import com.apple.wwrc.foundation.configuration.updater.AbstractScheduler;
import com.apple.wwrc.service.authorization.datasource.UserAccessPolicyRow;

public class UserAccessPolicyPullingScheduler extends AbstractScheduler<UserAccessPolicyRow> {
  //Arguments to pass onExecute( )
  private static AbstractLoader<UserAccessPolicyRow> loader;//Can't pass loader in as arguments
  private static AbstractRepository<UserAccessPolicyRow> repository;//Can't pass repository as arguments

  @Override
  public synchronized AbstractScheduler<UserAccessPolicyRow> setRepository(AbstractRepository<UserAccessPolicyRow> repo) {
    repository = repo;
    return this;
  }

  @Override
  public synchronized AbstractScheduler<UserAccessPolicyRow> setLoader(AbstractLoader<UserAccessPolicyRow> loaderr) {
    loader = loaderr;
    return this;
  }

  @Override
  public AbstractLoader<UserAccessPolicyRow> getLoader() {
    return loader;
  }

  @Override
  public AbstractRepository<UserAccessPolicyRow> getRepository() {
    return repository;
  }
}
