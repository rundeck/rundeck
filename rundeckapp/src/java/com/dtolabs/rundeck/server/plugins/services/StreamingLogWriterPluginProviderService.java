package com.dtolabs.rundeck.server.plugins.services;

import com.dtolabs.rundeck.core.plugins.BasePluggableProviderService;
import com.dtolabs.rundeck.core.plugins.ServiceProviderLoader;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.logging.StreamingLogWriterPlugin;

/** $INTERFACE is ... User: greg Date: 5/24/13 Time: 9:31 AM */
public class StreamingLogWriterPluginProviderService extends BasePluggableProviderService<StreamingLogWriterPlugin> {
    public static final String SERVICE_NAME = ServiceNameConstants.StreamingLogWriter;
    private ServiceProviderLoader rundeckServerServiceProviderLoader;

    public StreamingLogWriterPluginProviderService() {
        super(SERVICE_NAME, StreamingLogWriterPlugin.class);
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
