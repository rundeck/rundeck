package rundeck.services.optionsource

import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolver
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.plugins.option.OptionSourcePlugin
import grails.gorm.transactions.Transactional

@Transactional
class OptionSourceService {

    def pluginService
    def optionSourcePluginProviderService
    def frameworkService

    def getOptions() {
        def options = [:]
        pluginService.listPlugins(OptionSourcePlugin,optionSourcePluginProviderService).each { plugin ->
            options.putAll(doPlugin(plugin.key,frameworkService.getFrameworkPropertyResolver()))
        }
        return options
    }

    /**
     * Perform a plugin notification
     * @param trigger trigger name
     * @param data data content for the plugin
     * @param content content for notification
     * @param type plugin type
     * @param config user configuration
     */
    private Map doPlugin(String type, PropertyResolver resolver){

        //load plugin and configure with config values
        def result = pluginService.configurePlugin(type, optionSourcePluginProviderService, resolver, PropertyScope.Instance)
        if (!result?.instance) {
            return false
        }
        def plugin=result.instance
        /*
        * contains unmapped configuration values only
         */
        def config=result.configuration
        def allConfig = pluginService.getPluginConfiguration(type, optionSourcePluginProviderService, resolver, PropertyScope.Instance)

        plugin.optionMap
    }
}
