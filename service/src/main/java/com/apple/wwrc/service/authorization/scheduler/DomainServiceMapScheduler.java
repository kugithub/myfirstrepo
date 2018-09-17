package com.apple.wwrc.service.authorization.scheduler;

import com.apple.wwrc.foundation.configuration.updater.AbstractLoader;
import com.apple.wwrc.foundation.configuration.updater.AbstractRepository;
import com.apple.wwrc.foundation.configuration.updater.AbstractScheduler;
import com.apple.wwrc.service.authorization.datasource.DomainServiceMapRow;

@SuppressWarnings({"squid:S00103", "common-java:InsufficientCommentDensity", "squid:S1166", "squid:S109"})
public class DomainServiceMapScheduler extends AbstractScheduler<DomainServiceMapRow> {
  private static AbstractLoader<DomainServiceMapRow> loader;//Can't pass loader in as arguments
  private static AbstractRepository<DomainServiceMapRow> repository;//Can't pass repository as arguments

  @Override
  public AbstractLoader<DomainServiceMapRow> getLoader() {
    return loader;
  }

  @Override
  public AbstractRepository<DomainServiceMapRow> getRepository() {
    return repository;
  }

  @Override
  public synchronized AbstractScheduler<DomainServiceMapRow> setLoader(AbstractLoader<DomainServiceMapRow> loaderr) {
    loader = loaderr;
    return this;
  }

  @Override
  public synchronized AbstractScheduler<DomainServiceMapRow> setRepository(AbstractRepository<DomainServiceMapRow> repo) {
    repository = repo;
    return this;
  }

}
