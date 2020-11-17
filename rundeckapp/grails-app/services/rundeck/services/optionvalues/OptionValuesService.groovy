package rundeck.services.optionvalues

import com.dtolabs.rundeck.core.plugins.PluggableProviderService
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolver
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.plugins.option.OptionValue
import com.dtolabs.rundeck.plugins.option.OptionValuesPlugin
import org.rundeck.app.spi.Services

class OptionValuesService {

    def pluginService
    def rundeckPluginRegistry
    def frameworkService

    def getOptions(String project, String provider, Services services) {
        return doPlugin(provider,frameworkService.getFrameworkPropertyResolver(project,null), services)
    }

    private List<OptionValue> doPlugin(String provider, PropertyResolver resolver, Services services){
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
        rundeckPluginRegistry.createPluggableService(OptionValuesPlugin)
    }
}
