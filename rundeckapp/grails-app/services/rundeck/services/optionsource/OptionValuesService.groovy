package rundeck.services.optionsource

import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolver
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.plugins.option.OptionValue
import com.dtolabs.rundeck.plugins.option.OptionValuesPlugin
import grails.gorm.transactions.Transactional

@Transactional
class OptionValuesService {

    def pluginService
    def optionValuesPluginProviderService
    def frameworkService

    def getOptions(String provider) {
        println "using plugin: ${provider}"
        return doPlugin(provider,frameworkService.getFrameworkPropertyResolver())
    }

    /**
     * Perform a plugin notification
     * @param trigger trigger name
     * @param data data content for the plugin
     * @param content content for notification
     * @param type plugin type
     * @param config user configuration
     */
    private List<OptionValue> doPlugin(String type, PropertyResolver resolver){

        //load plugin and configure with config values
        def result = pluginService.configurePlugin(type, optionValuesPluginProviderService, resolver, PropertyScope.Instance)
        if (!result?.instance) {
            return false
        }
        def plugin=result.instance
        /*
        * contains unmapped configuration values only
         */
        def config=result.configuration
        def allConfig = pluginService.getPluginConfiguration(type, optionValuesPluginProviderService, resolver, PropertyScope.Instance)

        plugin.optionValues
    }

    Map listOptionValuesPlugins() {
        return pluginService.listPlugins(OptionValuesPlugin, optionValuesPluginProviderService)
    }
}
