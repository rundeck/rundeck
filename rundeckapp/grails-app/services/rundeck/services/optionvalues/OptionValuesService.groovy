package rundeck.services.optionvalues

import com.dtolabs.rundeck.core.plugins.PluggableProviderService
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolver
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.plugins.option.OptionValue
import com.dtolabs.rundeck.plugins.option.OptionValuesPlugin

class OptionValuesService {

    def pluginService
    def rundeckPluginRegistry
    def frameworkService

    def getOptions(String project, String provider) {
        return doPlugin(provider,frameworkService.getFrameworkPropertyResolver(project,null))
    }

    private List<OptionValue> doPlugin(String provider, PropertyResolver resolver){
        //load plugin and configure with config values
        def result = pluginService.configurePlugin(provider, getOptionValuesPluginService(), resolver, PropertyScope.Instance)
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
        rundeckPluginRegistry.createPluggableService(OptionValuesPlugin)
    }
}
