package com.dtolabs.rundeck.server.plugins.services;

import com.dtolabs.rundeck.core.plugins.BasePluggableProviderService;
import com.dtolabs.rundeck.core.plugins.ServiceProviderLoader;
import com.dtolabs.rundeck.core.resourcetree.ResourceConverter;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.logging.StreamingLogWriterPlugin;
import com.dtolabs.rundeck.plugins.resourcetree.ResourceConverterPlugin;

/**
 * $INTERFACE is ... User: greg Date: 2/19/14 Time: 2:25 PM
 */
public class ResourceConverterPluginProviderService extends BasePluggableProviderService<ResourceConverterPlugin> {
    public static final String SERVICE_NAME = ServiceNameConstants.ResourceConverter;
    private ServiceProviderLoader rundeckServerServiceProviderLoader;

    public ResourceConverterPluginProviderService() {
        super(SERVICE_NAME, ResourceConverterPlugin.class);
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
