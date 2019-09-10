package rundeck.services

import com.dtolabs.rundeck.core.encrypter.PasswordUtilityEncrypterPlugin
import com.dtolabs.rundeck.core.plugins.PluggableProviderService
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolver
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope


class PasswordUtilityEncrypterLoaderService {

    def rundeckPluginRegistry
    def pluginService
    def frameworkService

    def getPasswordEncoder(String provider, Map params) {
        return getPlugin(provider,frameworkService.getFrameworkPropertyResolverWithProps(params))
    }

    private def getPlugin(String provider, PropertyResolver resolver){
        //load plugin and configure with config values
        def result = pluginService.configurePlugin(provider, getPasswordUtilityEncrypterService(), resolver, PropertyScope.Instance)
        if (!result?.instance) {
            log.error("Plugin '${provider}' not found")
            return []
        }
        result
    }

    Map getPasswordUtilityEncrypters() {
        return pluginService.listPlugins(PasswordUtilityEncrypterPlugin, getPasswordUtilityEncrypterService())
    }

    PluggableProviderService<PasswordUtilityEncrypterPlugin> getPasswordUtilityEncrypterService() {
        rundeckPluginRegistry.createPluggableService(PasswordUtilityEncrypterPlugin)
    }
}