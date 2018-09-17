package com.apple.wwrc.service.authorization.scheduler;

import com.apple.wwrc.foundation.configuration.updater.AbstractLoader;
import com.apple.wwrc.foundation.configuration.updater.AbstractRepository;
import com.apple.wwrc.foundation.configuration.updater.AbstractScheduler;
import com.apple.wwrc.service.authorization.datasource.ServiceIdentityRow;

@SuppressWarnings({"squid:S00103", "common-java:InsufficientCommentDensity", "squid:S1166", "squid:S109"})
public class ServiceIdentityScheduler extends AbstractScheduler<ServiceIdentityRow> {
  private static AbstractLoader<ServiceIdentityRow> loader;//Can't pass loader in as arguments
  private static AbstractRepository<ServiceIdentityRow> repository;//Can't pass repository as arguments

  @Override
  public AbstractLoader<ServiceIdentityRow> getLoader() {
    return loader;
  }

  @Override
  public AbstractRepository<ServiceIdentityRow> getRepository() {
    return repository;
  }

  @Override
  public synchronized AbstractScheduler<ServiceIdentityRow> setLoader(AbstractLoader<ServiceIdentityRow> loaderr) {
    loader = loaderr;
    return this;
  }

  @Override
  public synchronized AbstractScheduler<ServiceIdentityRow> setRepository(AbstractRepository<ServiceIdentityRow> repo) {
    repository = repo;
    return this;
  }

}
