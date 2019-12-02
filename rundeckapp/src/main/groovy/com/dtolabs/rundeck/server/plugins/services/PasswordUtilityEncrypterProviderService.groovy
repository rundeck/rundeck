package com.dtolabs.rundeck.server.plugins.services

import com.dtolabs.rundeck.core.encrypter.PasswordUtilityEncrypterPlugin
import com.dtolabs.rundeck.core.plugins.BasePluggableProviderService
import com.dtolabs.rundeck.core.plugins.ServiceProviderLoader
import com.dtolabs.rundeck.plugins.ServiceNameConstants

class PasswordUtilityEncrypterProviderService extends BasePluggableProviderService<PasswordUtilityEncrypterPlugin> {
    ServiceProviderLoader pluginManager;

    PasswordUtilityEncrypterProviderService(final ServiceProviderLoader pluginManager) {
        super(ServiceNameConstants.PasswordUtilityEncrypter, PasswordUtilityEncrypterPlugin.class)
        this.pluginManager = pluginManager
    }

    @Override
    ServiceProviderLoader getPluginManager() {
        return pluginManager
    }


}