package rundeck.services.optionsource

import com.dtolabs.rundeck.core.plugins.PluggableProviderService
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolver
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.plugins.option.OptionValue
import com.dtolabs.rundeck.plugins.option.OptionValuesPlugin

class OptionValuesService {

    def pluginService
    def rundeckPluginRegistry
    def frameworkService

    def getOptions(String provider) {
        println "using plugin: ${provider}"
        return doPlugin(provider,frameworkService.getFrameworkPropertyResolver())
    }

    private List<OptionValue> doPlugin(String type, PropertyResolver resolver){
        println "doing plugin: ${type}"
        //load plugin and configure with config values
        def result = pluginService.configurePlugin(type, getOptionValuesPluginService(), resolver, PropertyScope.Instance)
        if (!result?.instance) {
            return false
        }
        def plugin=result.instance
        plugin.getOptionValues(result.configuration)
    }

    Map listOptionValuesPlugins() {
        return pluginService.listPlugins(OptionValuesPlugin, getOptionValuesPluginService())
    }

    PluggableProviderService<OptionValuesPlugin> getOptionValuesPluginService() {
        rundeckPluginRegistry.createPluggableService(OptionValuesPlugin)
    }
}
