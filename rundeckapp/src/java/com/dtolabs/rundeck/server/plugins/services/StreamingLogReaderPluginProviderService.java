package com.dtolabs.rundeck.server.plugins.services;

import com.dtolabs.rundeck.core.plugins.BasePluggableProviderService;
import com.dtolabs.rundeck.core.plugins.ServiceProviderLoader;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.logging.StreamingLogReaderPlugin;

/** $INTERFACE is ... User: greg Date: 5/24/13 Time: 9:32 AM */
public class StreamingLogReaderPluginProviderService extends BasePluggableProviderService<StreamingLogReaderPlugin> {
    public static final String SERVICE_NAME = ServiceNameConstants.StreamingLogReader;
    private ServiceProviderLoader rundeckServerServiceProviderLoader;

    public StreamingLogReaderPluginProviderService() {
        super(SERVICE_NAME, StreamingLogReaderPlugin.class);
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
