package com.dtolabs.rundeck.server.plugins.services;

import com.dtolabs.rundeck.core.plugins.BasePluggableProviderService;
import com.dtolabs.rundeck.core.plugins.ServiceProviderLoader;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.project.JobLifecyclePlugin;

public class JobLifecyclePluginProviderService extends BasePluggableProviderService<JobLifecyclePlugin> {

    public static final String SERVICE_NAME = ServiceNameConstants.JobLifecyclePlugin;
    private ServiceProviderLoader rundeckServerServiceProviderLoader;

    public JobLifecyclePluginProviderService() {
        super(SERVICE_NAME, JobLifecyclePlugin.class);
    }

    public ServiceProviderLoader getPluginManager() {
        return getRundeckServerServiceProviderLoader();
    }

    public ServiceProviderLoader getRundeckServerServiceProviderLoader() {
        return rundeckServerServiceProviderLoader;
    }

    public void setRundeckServerServiceProviderLoader(ServiceProviderLoader rundeckServerServiceProviderLoader) {
        this.rundeckServerServiceProviderLoader = rundeckServerServiceProviderLoader;
    }

}
