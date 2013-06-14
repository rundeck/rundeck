package com.dtolabs.rundeck.server.plugins.services;

import com.dtolabs.rundeck.core.plugins.BasePluggableProviderService;
import com.dtolabs.rundeck.core.plugins.ServiceProviderLoader;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.notification.NotificationPlugin;

/**
 * Provider service for NotificationPlugins
 * Created by greg
 * Date: 4/12/13
 * Time: 4:43 PM
 */
public class NotificationPluginProviderService extends BasePluggableProviderService<NotificationPlugin> {
    public static final String SERVICE_NAME = ServiceNameConstants.Notification;
    private ServiceProviderLoader rundeckServerServiceProviderLoader;

    public NotificationPluginProviderService() {
        super(SERVICE_NAME, NotificationPlugin.class);
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

    @Override
    public boolean isScriptPluggable() {
        //for now
        return false;
    }
}
