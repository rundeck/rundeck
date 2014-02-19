package com.dtolabs.rundeck.server.plugins.services;

import com.dtolabs.rundeck.core.plugins.BasePluggableProviderService;
import com.dtolabs.rundeck.core.plugins.ServiceProviderLoader;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.resourcetree.ResourceStoragePlugin;

/**
 */
public class ResourceStoragePluginProviderService extends BasePluggableProviderService<ResourceStoragePlugin> {
    public static final String SERVICE_NAME = ServiceNameConstants.ResourceStorage;
    private ServiceProviderLoader rundeckServerServiceProviderLoader;

    public ResourceStoragePluginProviderService() {
        super(SERVICE_NAME, ResourceStoragePlugin.class);
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
