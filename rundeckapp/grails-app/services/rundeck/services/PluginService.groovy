package rundeck.services

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.plugins.PluggableProviderService
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.server.plugins.RundeckPluginRegistry

class PluginService {

    def RundeckPluginRegistry rundeckPluginRegistry
    def frameworkService
    static transactional = false

    def <T> T getPlugin(String name, PluggableProviderService<T> service) {
        def bean = rundeckPluginRegistry?.loadPluginByName(name, service)
        if (bean != null) {
            return (T) bean
        }
        log.error("${service.name} plugin not found: ${name}")
        return bean
    }

    def Map validatePluginConfig(String name, Map config, PluggableProviderService service) {
        def Map pluginDesc = getPluginDescriptor(name,service)
        if (pluginDesc && pluginDesc.description instanceof Description) {
            return frameworkService.validateDescription(pluginDesc.description, '', config)
        } else {
            return null
        }
    }
    /**
     *
     * @param name
     * @return map containing [instance:(plugin instance), description: (map or Description), ]
     */
    def Map getPluginDescriptor(String name, PluggableProviderService service) {
        def bean = rundeckPluginRegistry?.loadPluginDescriptorByName(name, service)
        if (bean) {
            return (Map) bean
        }
        log.error("${service.name} not found: ${name}")
        return null
    }

    def <T> T configurePlugin(String name, Map configuration, PluggableProviderService<T> service) {
        def bean = rundeckPluginRegistry?.configurePluginByName(name, service, configuration)
        if (bean) {
            return (T) bean
        }
        log.error("${service.name} not found: ${name}")
        return null
    }

    def <T> T configurePlugin(String name, Map configuration, String projectName, Framework framework, PluggableProviderService<T> service) {
        def bean = rundeckPluginRegistry?.configurePluginByName(name, service, configuration)
        if (bean) {
            return (T) bean
        }
        log.error("${service.name} not found: ${name}")
        return null
    }

    def <T> Map listPlugins(PluggableProviderService<T> service) {
        def plugins = [:]
        plugins = rundeckPluginRegistry?.listPluginDescriptors(T, service)
        //clean up name of any Groovy plugin without annotations that ends with the service name
        plugins.each { key, Map plugin ->
            def desc = plugin.description
            if (desc && desc instanceof Map) {
                if (desc.name.endsWith(service.name)) {
                    desc.name = desc.name.substring(service.name.length())
                }
            }
        }
//        System.err.println("listed plugins: ${plugins}")

        plugins
    }
}
