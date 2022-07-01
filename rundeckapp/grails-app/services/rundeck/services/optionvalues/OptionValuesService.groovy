package rundeck.services.optionvalues

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.plugins.PluggableProviderService
import com.dtolabs.rundeck.core.plugins.ServiceProviderLoader
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolver
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.core.storage.keys.KeyStorageTree
import com.dtolabs.rundeck.plugins.option.OptionValue
import com.dtolabs.rundeck.plugins.option.OptionValuesPlugin
import org.rundeck.app.spi.RundeckSpiBaseServicesProvider

class OptionValuesService {

    def pluginService
    def ServiceProviderLoader rundeckServerServiceProviderLoader
    def frameworkService
    def storageService

    def getOptions(String project, String provider, AuthContext authContext) {
        return doPlugin(provider,frameworkService.getFrameworkPropertyResolver(project,null), authContext)
    }

    private List<OptionValue> doPlugin(String provider, PropertyResolver resolver, AuthContext authContext){

        KeyStorageTree storageTree = storageService.storageTreeWithContext(authContext)
        Map<Class, Object> servicesMap = [:]
        servicesMap.put(KeyStorageTree, storageTree)

        def services = new RundeckSpiBaseServicesProvider(
                services: servicesMap
        )

        //load plugin and configure with config values
        def result = pluginService.configurePlugin(provider, getOptionValuesPluginService(), resolver, PropertyScope.Instance, services)
        if (!result?.instance) {
            log.error("Plugin '${provider}' not found")
            return []
        }
        def plugin=result.instance
        plugin.getOptionValues(result.configuration)
    }

    Map listOptionValuesPlugins() {
        return pluginService.listPlugins(OptionValuesPlugin, getOptionValuesPluginService())
    }

    PluggableProviderService<OptionValuesPlugin> getOptionValuesPluginService() {
        rundeckServerServiceProviderLoader.createPluggableService(OptionValuesPlugin)
    }
}
