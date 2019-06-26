package com.dtolabs.rundeck.server.plugins.services

import com.dtolabs.rundeck.core.encrypter.PasswordUtilityEncrypter
import com.dtolabs.rundeck.core.plugins.BasePluggableProviderService
import com.dtolabs.rundeck.core.plugins.ServiceProviderLoader
import com.dtolabs.rundeck.plugins.ServiceNameConstants

class PasswordUtilityEncrypterProviderService extends BasePluggableProviderService<PasswordUtilityEncrypter> {
    ServiceProviderLoader pluginManager;

    PasswordUtilityEncrypterProviderService(final ServiceProviderLoader pluginManager) {
        super(ServiceNameConstants.PasswordUtilityEncrypter, PasswordUtilityEncrypter.class)
        this.pluginManager = pluginManager
    }

    @Override
    ServiceProviderLoader getPluginManager() {
        return pluginManager
    }


}
