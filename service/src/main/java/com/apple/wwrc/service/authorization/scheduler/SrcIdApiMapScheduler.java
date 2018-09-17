package com.apple.wwrc.service.authorization.scheduler;

import com.apple.wwrc.foundation.configuration.updater.AbstractLoader;
import com.apple.wwrc.foundation.configuration.updater.AbstractRepository;
import com.apple.wwrc.foundation.configuration.updater.AbstractScheduler;
import com.apple.wwrc.service.authorization.datasource.SrcIdApiMapRow;

@SuppressWarnings({"squid:S00103", "common-java:InsufficientCommentDensity", "squid:S1166", "squid:S109"})
public class SrcIdApiMapScheduler extends AbstractScheduler<SrcIdApiMapRow> {
  private static AbstractLoader<SrcIdApiMapRow> loader;//Can't pass loader in as arguments
  private static AbstractRepository<SrcIdApiMapRow> repository;//Can't pass repository as arguments

  @Override
  public AbstractLoader<SrcIdApiMapRow> getLoader() {
    return loader;
  }

  @Override
  public AbstractRepository<SrcIdApiMapRow> getRepository() {
    return repository;
  }

  @Override
  public synchronized AbstractScheduler<SrcIdApiMapRow> setLoader(AbstractLoader<SrcIdApiMapRow> loaderr) {
    loader = loaderr;
    return this;
  }

  @Override
  public synchronized AbstractScheduler<SrcIdApiMapRow> setRepository(AbstractRepository<SrcIdApiMapRow> repo) {
    repository = repo;
    return this;
  }
}
