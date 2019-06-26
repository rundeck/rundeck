package rundeck.services

import com.dtolabs.rundeck.core.plugins.PluggableProviderService
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolver
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.core.encrypter.PasswordUtilityEncrypter


class PasswordUtilityEncrypterLoaderService {

    def rundeckPluginRegistry
    def pluginService
    def frameworkService

    def getPasswordEncoder(String provider) {
        return getPlugin(provider,frameworkService.getFrameworkPropertyResolverWithProps())
    }

    private PasswordUtilityEncrypter getPlugin(String provider, PropertyResolver resolver){
        //load plugin and configure with config values
        def result = pluginService.configurePlugin(provider, getPasswordUtilityEncrypterService(), resolver, PropertyScope.Instance)
        if (!result?.instance) {
            log.error("Plugin '${provider}' not found")
            return []
        }
        def plugin=result.instance
        plugin
    }

    Map listPlugins(){
        def result = new HashMap()
        def list = getPasswordUtilityEncrypters()

        list.each({it->
            def plugin = getPasswordEncoder(it.key)
            result.put(it.key, plugin)
        })
        result
    }

    Map getPasswordUtilityEncrypters() {
        return pluginService.listPlugins(PasswordUtilityEncrypter, getPasswordUtilityEncrypterService())
    }

    PluggableProviderService<PasswordUtilityEncrypter> getPasswordUtilityEncrypterService() {
        rundeckPluginRegistry.createPluggableService(PasswordUtilityEncrypter)
    }
}
