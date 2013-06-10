package rundeck.services

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolver
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolverFactory
import com.dtolabs.rundeck.core.plugins.PluggableProviderService
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.core.plugins.configuration.Validator
import com.dtolabs.rundeck.server.plugins.PluginRegistry

class PluginService {

    def PluginRegistry rundeckPluginRegistry
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
        if (bean != null) {
            return (Map) bean
        }
        log.error("${service.name} not found: ${name}")
        return null
    }

    /**
     * Configure a plugin given only instance configuration
     * @param name name
     * @param configuration instance configuration
     * @param service service
     * @return plugin, or null if configuration or plugin loading failed
     */
    def <T> T configurePlugin(String name, Map configuration, PluggableProviderService<T> service) {
        def validation = rundeckPluginRegistry?.validatePluginByName(name, service, configuration)
        if (validation != null && !validation.valid) {
            logValidationErrors(service.name, name, validation.report )
            return null
        }
        def bean = rundeckPluginRegistry?.configurePluginByName(name, service, configuration)
        if (bean != null) {
            return (T) bean
        }
        log.error("${service.name} not found: ${name}")
        return null
    }

    /**
     * Configure a new plugin using only instance-scope configuration values
     * @param name provider name
     * @param configuration map of instance configuration values
     * @param service service
     * @return configured plugin instance, or null if not found
     */
    def <T> T configurePlugin(String name, Map configuration, String projectName, Framework framework, PluggableProviderService<T> service) {
        def validation = rundeckPluginRegistry?.validatePluginByName(name, service, framework, projectName, configuration)
        if (!validation.valid) {
            logValidationErrors(service.name, name, validation.report)
            return null
        }
        def bean = rundeckPluginRegistry?.configurePluginByName(name, service, framework, projectName, configuration)
        if (bean != null) {
            return (T) bean
        }
        log.error("${service.name} not found: ${name}")
        return null
    }

    /**
     * Configure a new plugin using a specific property resolver for configuration
     * @param name provider name
     * @param service service
     * @param resolver property resolver for configuration properties
     * @param defaultScope default plugin property scope
     * @return configured plugin instance, or null if not found
     */
    def <T> T configurePlugin(String name, PluggableProviderService<T> service, PropertyResolver resolver, PropertyScope defaultScope) {
        def validation = rundeckPluginRegistry?.validatePluginByName(name, service,
                PropertyResolverFactory.createPrefixedResolver(resolver, name, service.name), defaultScope)
        if(!validation.valid) {
            logValidationErrors(service.name, name, validation.report)
            return null
        }
        def bean = rundeckPluginRegistry?.configurePluginByName(name, service,
                PropertyResolverFactory.createPrefixedResolver(resolver, name, service.name), defaultScope)

        if (bean != null) {
            return (T) bean
        }
        log.error("${service.name} not found: ${name}")
        return null
    }

    private void logValidationErrors(String svcName, String pluginName,Validator.Report report) {
        def sb = new StringBuilder()
        sb<< "${svcName}: configuration was not valid for plugin '${pluginName}': "
        report?.errors.each { k, v ->
            sb<<"${k}: ${v}\n"
        }
        log.error(sb.toString())
    }

    /**
     * Configure a new plugin using a specific property resolver for configuration
     * @param name provider name
     * @param service service
     * @param resolver property resolver for configuration properties
     * @param defaultScope default plugin property scope
     * @return configured plugin instance, or null if not found
     */
    def Map validatePlugin(String name, PluggableProviderService service, PropertyResolver resolver, PropertyScope defaultScope) {
        return rundeckPluginRegistry?.validatePluginByName(name, service,
                PropertyResolverFactory.createPrefixedResolver(resolver, name, service.name), defaultScope)
    }

    def <T> Map listPlugins(PluggableProviderService<T> service) {
        def plugins = rundeckPluginRegistry?.listPluginDescriptors(T, service)
        //clean up name of any Groovy plugin without annotations that ends with the service name
        plugins.each { key, Map plugin ->
            def desc = plugin.description
            if (desc && desc instanceof Map) {
                if (desc.name.endsWith(service.name)) {
                    desc.name = desc.name.substring(0,desc.name.length()-service.name.length())
                }
            }
        }
//        System.err.println("listed plugins: ${plugins}")

        plugins
    }
}
