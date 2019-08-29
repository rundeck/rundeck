package com.dtolabs.rundeck.server.plugins.services;

import com.dtolabs.rundeck.core.plugins.BasePluggableProviderService;
import com.dtolabs.rundeck.core.plugins.ServiceProviderLoader;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.project.ProjectPlugin;

public class ProjectPluginProviderService extends BasePluggableProviderService<ProjectPlugin> {

    public static final String SERVICE_NAME = ServiceNameConstants.ProjectPlugin;
    private ServiceProviderLoader rundeckServerServiceProviderLoader;

    public ProjectPluginProviderService() {
        super(SERVICE_NAME, ProjectPlugin.class);
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
