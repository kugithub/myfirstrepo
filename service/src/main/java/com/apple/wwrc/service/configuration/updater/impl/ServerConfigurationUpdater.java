package com.apple.wwrc.service.configuration.updater.impl;

import com.apple.wwrc.foundation.configuration.repository.ConfigurationRow;
import com.apple.wwrc.foundation.configuration.updater.AbstractLoader;
import com.apple.wwrc.foundation.configuration.updater.AbstractRepository;
import com.apple.wwrc.foundation.configuration.updater.AbstractScheduler;

public class ServerConfigurationUpdater extends AbstractScheduler<ConfigurationRow> {
    //Arguments to pass onExecute( )
    private static AbstractLoader<ConfigurationRow> loader;//Can't pass loader in as arguments
    private static AbstractRepository<ConfigurationRow> repository;//Can't pass repository as arguments
    
    public synchronized AbstractScheduler<ConfigurationRow> setRepository(AbstractRepository<ConfigurationRow> repo) {
        repository = repo;
        return this;
    }
    public synchronized AbstractScheduler<ConfigurationRow> setLoader(AbstractLoader<ConfigurationRow> loaderr) {
        loader = loaderr;
        return this;
    }
    public AbstractLoader<ConfigurationRow> getLoader() {
        return loader;
    }
    public AbstractRepository<ConfigurationRow> getRepository() {
        return repository;
    }
}
