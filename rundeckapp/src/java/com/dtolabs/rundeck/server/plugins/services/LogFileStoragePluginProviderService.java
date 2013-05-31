package com.dtolabs.rundeck.server.plugins.services;

import com.dtolabs.rundeck.core.plugins.BasePluggableProviderService;
import com.dtolabs.rundeck.core.plugins.ServiceProviderLoader;
import com.dtolabs.rundeck.plugins.logging.LogFileStoragePlugin;

/** $INTERFACE is ... User: greg Date: 5/29/13 Time: 1:40 PM */
public class LogFileStoragePluginProviderService extends BasePluggableProviderService<LogFileStoragePlugin> {
    public static final String SERVICE_NAME = "LogFileStorage";
    private ServiceProviderLoader rundeckServerServiceProviderLoader;

    public LogFileStoragePluginProviderService() {
        super(SERVICE_NAME, LogFileStoragePlugin.class);
    }

    @Override
    public ServiceProviderLoader getPluginManager() {
        return getRundeckServerServiceProviderLoader();
    }

    public ServiceProviderLoader getRundeckServerServiceProviderLoader() {
        return rundeckServerServiceProviderLoader;
    }

    public void setRundeckServerServiceProviderLoader(ServiceProviderLoader rundeckServerServiceProviderLoader) {
        this.rundeckServerServiceProviderLoader = rundeckServerServiceProviderLoader;
    }

    @Override
    public boolean isScriptPluggable() {
        //for now
        return false;
    }
}
