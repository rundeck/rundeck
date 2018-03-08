/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rundeck.services

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.IFramework
import com.dtolabs.rundeck.core.plugins.CloseableProvider
import com.dtolabs.rundeck.core.plugins.MultiPluginProviderLoader
import com.dtolabs.rundeck.core.plugins.SimplePluginProviderLoader
import com.dtolabs.rundeck.core.plugins.configuration.DynamicProperties
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolver
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolverFactory
import com.dtolabs.rundeck.core.plugins.PluggableProviderService
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.core.plugins.configuration.Validator
import com.dtolabs.rundeck.plugins.ServiceTypes
import com.dtolabs.rundeck.server.plugins.ConfiguredPlugin
import com.dtolabs.rundeck.server.plugins.DescribedPlugin
import com.dtolabs.rundeck.server.plugins.PluginRegistry
import com.dtolabs.rundeck.server.plugins.RenamedDescription
import com.dtolabs.rundeck.server.plugins.ValidatedPlugin

class PluginService {

    def PluginRegistry rundeckPluginRegistry
    def frameworkService
    static transactional = false

    /**
     * Get a plugin
     * @param name
     * @param service
     * @return
     */
    def <T> T getPlugin(String name, Class<T> type) {
        getPlugin(name, rundeckPluginRegistry?.createPluggableService(type))
    }
    def <T> T getPlugin(String name, PluggableProviderService<T> service) {
        def bean = rundeckPluginRegistry?.loadPluginByName(name, service)
        if (bean != null) {
            return (T) bean
        }
        log.error("${service.name} plugin not found: ${name}")
        return bean
    }

    /**
     * Load a plugin which can be closed when no longer in use
     * @param name
     * @param service
     * @return
     */
    def <T> CloseableProvider<T> retainPlugin(String name, PluggableProviderService<T> service) {
        def bean = rundeckPluginRegistry?.retainPluginByName(name, service)
        if (bean != null) {
            return (CloseableProvider<T>) bean
        }
        log.error("${service.name} plugin not found: ${name}")
        return bean
    }

    /**
     *
     * @param name
     * @return map containing [instance:(plugin instance), description: (map or Description), ]
     */
    def DescribedPlugin getPluginDescriptor(String name, PluggableProviderService service) {
        def bean = rundeckPluginRegistry?.loadPluginDescriptorByName(name, service)
        if (bean != null) {
            return bean
        }
        log.error("${service.name} not found: ${name}")
        return null
    }
    /**
     *
     * @param name
     * @return map containing [instance:(plugin instance), description: (map or Description), ]
     */
    def DescribedPlugin getPluginDescriptor(String name, String service) {
        getPluginDescriptor(name, getPluginTypeByService(service))
    }
    /**
     *
     * @param name
     * @return map containing [instance:(plugin instance), description: (map or Description), ]
     */
    def Map<String, Object> getPluginDynamicProperties(String name, String service) {
        def plugin = getPlugin(name, getPluginTypeByService(service))
        if (!(plugin instanceof DynamicProperties)) {
            return [:]
        }
        DynamicProperties dynamicProperties = (DynamicProperties) plugin
        //TODO: load project/fwk
        dynamicProperties.dynamicProperties(null)
    }

    /**
     * Return the java class associated with the given service name, or throw exception if not available
     * @param service
     * @throws IllegalArgumentException
     */
    public Class<?> getPluginTypeByService(String service) throws IllegalArgumentException {
        if (!ServiceTypes.getPluginType(service)) {
            throw new IllegalArgumentException("Unknown service: " + service)
        }
        ServiceTypes.getPluginType(service)
    }
    /**
     *
     * @param name
     * @return map containing [instance:(plugin instance), description: (map or Description), ]
     */
    def DescribedPlugin getPluginDescriptor(String name, Class type) {
        getPluginDescriptor(name, rundeckPluginRegistry?.createPluggableService(type))
    }

    /**
     * Configure a plugin given only instance configuration
     * @param name name
     * @param configuration instance configuration
     * @param service service
     * @return plugin, or null if configuration or plugin loading failed
     */
    def <T> ConfiguredPlugin<T> configurePlugin(String name, Map configuration, PluggableProviderService<T> service) {
        configurePlugin(name, configuration, service, (String) null)

    }
    /**
     * Configure a plugin given only instance configuration
     * @param name name
     * @param configuration instance configuration
     * @param service service
     * @return plugin , or null if configuration or plugin loading failed
     */
    def <T> ConfiguredPlugin<T> configurePlugin(
        String name,
        Map configuration,
        PluggableProviderService<T> service,
        String projectName
    ) {
        configurePlugin(name, configuration, service, projectName ? createMultiPluginLoader(projectName) : null)
    }
    /**
     * Configure a plugin given only instance configuration
     * @param name name
     * @param configuration instance configuration
     * @param service service
     * @param loader plugin loader
     * @return plugin , or null if configuration or plugin loading failed
     */
    def <T> ConfiguredPlugin<T> configurePlugin(
        String name,
        Map configuration,
        PluggableProviderService<T> service,
        MultiPluginProviderLoader loader
    ) {
        def validation = rundeckPluginRegistry?.validatePluginByName(name, service, configuration)
        if (validation != null && !validation.valid) {
            logValidationErrors(service.name, name, validation.report)
            return null
        }
        def result = rundeckPluginRegistry?.configurePluginByName(
            name,
            service,
            configuration,
            loader
        )
        if (result?.instance != null) {
            return result
        }
        log.error("${service.name} not found: ${name}")
        return null
    }
    /**
     * Configure a plugin given only instance configuration
     * @param name name
     * @param configuration instance configuration
     * @param service service
     * @return plugin , or null if configuration or plugin loading failed
     */
    def <T> ConfiguredPlugin<T> configurePlugin(String name, Map configuration, Class<T> type) {
        configurePlugin(name, configuration, type, (String) null)
    }
    /**
     * Configure a plugin given only instance configuration
     * @param name name
     * @param configuration instance configuration
     * @param service service
     * @return plugin , or null if configuration or plugin loading failed
     */
    def <T> ConfiguredPlugin<T> configurePlugin(String name, Map configuration, Class<T> type, String projectName) {
        configurePlugin(name, configuration, rundeckPluginRegistry?.createPluggableService(type), projectName)
    }

    def <T> SimplePluginProviderLoader<T> createSimplePluginLoader(
        String projectName,
        Framework framework,
        PluggableProviderService<T> service
    ) {
        return { String provider, Map<String, Object> config ->
            def plugin = configurePlugin(provider, (Map) config, projectName, framework, service)
            plugin?.instance
        } as SimplePluginProviderLoader<T>
    }

    /**
     * Create a multi plugin loader for the given project and framework
     * @param projectName
     * @param framework
     * @return
     */
    MultiPluginProviderLoader createMultiPluginLoader(String projectName) {
        return new RDMultiPluginLoader(
            pluginService: this,
            projectName: projectName,
            framework: frameworkService.rundeckFramework
        )
    }

    /**
     * Configure a new plugin using only instance-scope configuration values
     * @param name provider name
     * @param configuration map of instance configuration values
     * @param service service
     * @return Map of [instance: plugin instance, configuration: Map of resolved configuration properties], or null
     */
    def <T> ConfiguredPlugin<T> configurePlugin(
        String name,
        Map configuration,
        String projectName,
        Framework framework,
        PluggableProviderService<T> service
    ) {
        configurePlugin(
            name,
            configuration,
            projectName,
            framework,
            service,
            createMultiPluginLoader(projectName)
        )
    }
    /**
     * Configure a new plugin using only instance-scope configuration values
     * @param name provider name
     * @param configuration map of instance configuration values
     * @param service service
     * @return Map of [instance: plugin instance, configuration: Map of resolved configuration properties], or null
     */
    def <T> ConfiguredPlugin<T> configurePlugin(
        String name,
        Map configuration,
        String projectName,
        IFramework framework,
        PluggableProviderService<T> service,
        MultiPluginProviderLoader loader
    ) {
        //TODO: validate with loader?
        def validation = rundeckPluginRegistry?.validatePluginByName(name, service, framework, projectName, configuration)
        if (!validation) {
            logValidationErrors(service.name, name, Validator.errorReport('provider', 'Not found: ' + name))
            return null
        }
        if (!validation.valid) {
            logValidationErrors(service.name, name, validation.report)
            return null
        }
        def result = rundeckPluginRegistry?.configurePluginByName(
            name,
            service,
            framework,
            projectName,
            configuration,
            loader
        )
        if (result.instance != null) {
            return result
        }
        log.error("${service.name} not found: ${name}")
        return null
    }
    /**
     * Configure a new plugin using only instance-scope configuration values
     * @param name provider name
     * @param configuration map of instance configuration values
     * @param service service
     * @return Map of [instance: plugin instance, configuration: Map of resolved configuration properties], or null
     */
    def <T> ConfiguredPlugin<T> configurePlugin(
        String name,
        Map configuration,
        String projectName,
        Framework framework,
        Class<T> type
    )
    {
        configurePlugin(
                name,
                configuration,
                projectName,
                framework,
                rundeckPluginRegistry?.createPluggableService(type)
        )
    }

    /**
     * Configure a new plugin using a specific property resolver for configuration
     * @param name provider name
     * @param service service
     * @param resolver property resolver for configuration properties
     * @param defaultScope default plugin property scope
     * @return Map of [instance: plugin instance, configuration: Map of resolved configuration properties], or null
     */
    def <T> ConfiguredPlugin<T> configurePlugin(
            String name,
            PluggableProviderService<T> service,
            PropertyResolver resolver,
            PropertyScope defaultScope
    ) {
        configurePlugin(name, service, resolver, defaultScope, null)
    }
    /**
     * Configure a new plugin using a specific property resolver for configuration
     * @param name provider name
     * @param service service
     * @param resolver property resolver for configuration properties
     * @param defaultScope default plugin property scope
     * @return Map of [instance: plugin instance, configuration: Map of resolved configuration properties], or null
     */
    def <T> ConfiguredPlugin<T> configurePlugin(
        String name,
        PluggableProviderService<T> service,
        PropertyResolver resolver,
        PropertyScope defaultScope,
        MultiPluginProviderLoader loader
    )
    {
        def validation = rundeckPluginRegistry?.validatePluginByName(
            name,
            service,
            PropertyResolverFactory.createPrefixedResolver(resolver, name, service.name),
            defaultScope
        )
        if(null==validation){
            return null
        }
        if(!validation.valid) {
            logValidationErrors(service.name, name, validation.report)
            return null
        }
        def result = rundeckPluginRegistry?.configurePluginByName(
            name,
            service,
            PropertyResolverFactory.createPrefixedResolver(resolver, name, service.name),
            defaultScope,
            loader
        )

        if (result.instance != null) {
            return result
        }
        log.error("${service.name} not found: ${name}")
        return null
    }

    def <T> ConfiguredPlugin<T> configurePlugin(
            String name,
            Class<T> type,
            PropertyResolver resolver,
            PropertyScope defaultScope
    )
    {
        configurePlugin(name, rundeckPluginRegistry?.createPluggableService(type), resolver, defaultScope)
    }

    /**
     * Return the configured values for a plugin
     * @param name provider name
     * @param service service
     * @param resolver property resolver for configuration properties
     * @param defaultScope default plugin property scope
     * @return Map of [instance: plugin instance, configuration: Map of resolved configuration properties], or null
     */
    def <T> Map getPluginConfiguration(
        String name,
        PluggableProviderService<T> service,
        PropertyResolver resolver,
        PropertyScope defaultScope
    ) {
        return rundeckPluginRegistry?.getPluginConfigurationByName(
            name,
            service,
            PropertyResolverFactory.createPrefixedResolver(resolver, name, service.name),
            defaultScope
        )
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
     * @return validation
     */
    def ValidatedPlugin validatePlugin(String name, PluggableProviderService service, PropertyResolver resolver,
                         PropertyScope defaultScope) {
        return rundeckPluginRegistry?.validatePluginByName(name, service,
                PropertyResolverFactory.createPrefixedResolver(resolver, name, service.name), defaultScope)
    }
    /**
     * Configure a new plugin using a specific property resolver for configuration
     * @param name provider name
     * @param service service
     * @param resolver property resolver for configuration properties
     * @param defaultScope default plugin property scope
     * @param ignoredScope ignored scope
     * @return validation
     */
    def ValidatedPlugin validatePlugin(String name, PluggableProviderService service, PropertyResolver resolver, PropertyScope defaultScope, PropertyScope ignoredScope) {
        return rundeckPluginRegistry?.validatePluginByName(name, service,
                PropertyResolverFactory.createPrefixedResolver(resolver, name, service.name), defaultScope, ignoredScope)
    }

    /**
     * Configure a new plugin using a specific property resolver for configuration
     * @param name provider name
     * @param service service
     * @param config instance configuration data
     * @return validation
     */
    def ValidatedPlugin validatePluginConfig(String name, PluggableProviderService service, Map config) {
        return rundeckPluginRegistry?.validatePluginByName(name, service, config)
    }

    /**
     * Configure a new plugin using a specific property resolver for configuration
     * @param name provider name
     * @param service service
     * @param config instance configuration data
     * @return validation
     */
    def ValidatedPlugin validatePluginConfig(String name, Class clazz, Map config) {
        return rundeckPluginRegistry?.validatePluginByName(name, rundeckPluginRegistry?.createPluggableService(clazz), config)
    }

    def <T> Map<String, DescribedPlugin<T>> listPlugins(Class<T> clazz) {
        listPlugins(clazz, rundeckPluginRegistry?.createPluggableService(clazz))
    }
    /**
     * @param clazz
     * @param service
     * @return map of [name: DescribedPlugin]
     */
    def <T> Map<String, DescribedPlugin<T>> listPlugins(Class<T> clazz,PluggableProviderService<T> service) {
        def plugins = rundeckPluginRegistry?.listPluginDescriptors(clazz, service)
        //XX: avoid groovy bug where generic types referenced in closure can cause NPE: http://jira.codehaus.org/browse/GROOVY-5034
        String svcName=service.name
        //clean up name of any Groovy plugin without annotations that ends with the service name
        plugins.each { key, DescribedPlugin plugin ->
            def desc = plugin.description
            if(plugin.file && plugin.name.endsWith(svcName)){
                def newname = plugin.name
                newname=plugin.name.substring(0, plugin.name.length() - svcName.length())
                plugin.name = newname
                if (desc?.name?.endsWith(svcName)) {
                    plugin.description = new RenamedDescription(delegate: desc, name: newname)
                }
            }
        }
//        System.err.println("listed plugins: ${plugins}")

        plugins
    }
}

class RDMultiPluginLoader implements MultiPluginProviderLoader {
    PluginService pluginService
    String projectName
    IFramework framework

    @Override
    def <T> T load(final Class<T> clazz, final String provider, final Map<String, Object> config) {
        def service = pluginService.rundeckPluginRegistry?.createPluggableService(clazz)
        def plugin = pluginService.configurePlugin(provider, config, projectName, framework, service, this)
        plugin?.instance
    }
}
