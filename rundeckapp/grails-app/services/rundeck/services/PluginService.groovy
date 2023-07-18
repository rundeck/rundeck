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
import com.dtolabs.rundeck.core.common.PropertyRetriever
import com.dtolabs.rundeck.core.config.Features
import com.dtolabs.rundeck.core.plugins.CloseableProvider
import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.plugins.PluginConfigureService
import com.dtolabs.rundeck.core.plugins.PluginServiceCapabilities
import com.dtolabs.rundeck.core.plugins.SimplePluginProviderLoader
import com.dtolabs.rundeck.core.plugins.configuration.AcceptsServices
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.core.plugins.configuration.DynamicProperties
import com.dtolabs.rundeck.core.plugins.configuration.PluginAdapterUtility
import com.dtolabs.rundeck.core.plugins.configuration.Property
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolver
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolverFactory
import com.dtolabs.rundeck.core.plugins.PluggableProviderService
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.core.plugins.configuration.Validator
import com.dtolabs.rundeck.core.plugins.ConfiguredPlugin
import com.dtolabs.rundeck.core.plugins.DescribedPlugin
import com.dtolabs.rundeck.core.plugins.PluginRegistry
import com.dtolabs.rundeck.core.resources.format.ResourceFormatGenerator
import com.dtolabs.rundeck.core.resources.format.ResourceFormatParser
import com.dtolabs.rundeck.core.resources.format.ResourceFormats
import com.dtolabs.rundeck.core.resources.format.UnsupportedFormatException
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.ServiceTypes
import com.dtolabs.rundeck.plugins.config.ConfiguredBy
import com.dtolabs.rundeck.plugins.config.PluginGroup
import com.dtolabs.rundeck.plugins.storage.StoragePlugin
import com.dtolabs.rundeck.core.plugins.ValidatedPlugin
import groovy.transform.CompileStatic
import org.rundeck.app.spi.Services

@CompileStatic
class PluginService implements ResourceFormats, PluginConfigureService, PluginServiceCapabilities {

    def PluginRegistry rundeckPluginRegistry
    def FrameworkService frameworkService
    static transactional = false

    /**
     * Get a plugin
     * @param name
     * @param service
     * @return
     */
    def <T> T getPlugin(String name, Class<T> type) {
        getPlugin(name, createPluggableService(type))
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
    def DescribedPlugin getPluginDescriptor(String name, String service) {
        getPluginDescriptor(name, getPluginTypeByService(service))
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

    boolean hasPluginService(String service){
        return ServiceTypes.getPluginType(service)!=null
    }

    /**
     * Return the map of Java plugin interface class associated with service name
     * @param service
     * @throws IllegalArgumentException
     */
    public Map<Class<?>, String> getPluginTypesMap() {
        Map<Class<?>, String> types = [:]
        //reverse the map
        ServiceTypes.pluginTypesMap.each {
            types[it.value] = it.key
        }
        types
    }

    /**
     * Configure a new plugin using a specific property resolver for configuration
     * @param service service
     * @param provider provider name
     * @param config instance configuration data
     * @return validation
     */
    def ValidatedPlugin validatePluginConfig(String service, String provider, Map config) {
        Class serviceType = getPluginTypeByService(service)
        PluggableProviderService providerService = createPluggableService((Class) serviceType)
        return rundeckPluginRegistry?.validatePluginByName(provider, providerService, config)
    }
    /**
     * Configure a new plugin using a specific property resolver for configuration
     * @param service service
     * @param provider provider name
     * @param config instance configuration data
     * @param ignoredScope property scope to ignore
     * @return validation
     */
    def ValidatedPlugin validatePluginConfig(String service, String provider, Map config, PropertyScope ignoredScope) {
        Class serviceType = getPluginTypeByService(service)
        PluggableProviderService providerService = createPluggableService((Class) serviceType)
        return rundeckPluginRegistry?.validatePluginByName(provider, providerService, config, ignoredScope)
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
        log.warn("${service.name} not found: ${name}")
        return null
    }
    /**
     *
     * @param name
     * @return map containing [instance:(plugin instance), description: (map or Description), ]
     */
    def DescribedPlugin getPluginDescriptor(String name, Class type) {
        getPluginDescriptor(name, createPluggableService(type))
    }

    /**
     * load the dynamic select values for properties from the plugin, or null
     * @param serviceName
     * @param type
     * @param project
     * @param services
     * @return
     */
    def Map<String, Object> getDynamicProperties(
        IFramework rundeckFramework,
        String serviceName,
        String type,
        String project,
        Services services
    ) {

        PropertyRetriever retriever = PropertyResolverFactory.instanceRetriever([:])
        PropertyRetriever projectRetriever = PropertyResolverFactory.instanceRetriever(rundeckFramework.getFrameworkProjectMgr().getFrameworkProject(project).getProperties())
        PropertyResolverFactory.Factory resolverFactory = PropertyResolverFactory.pluginPrefixedScoped(retriever,projectRetriever,rundeckFramework.getPropertyRetriever())
        def plugin = configurePluginWithoutValidation(type, createPluggableService(getPluginTypeByService(serviceName)), resolverFactory, PropertyScope.Instance, null)

        def instance=plugin?.instance
        if(!(instance instanceof DynamicProperties)){
            return null
        }

        try{
            Map<String, Object> dynamicProperties = instance.dynamicProperties(plugin.configuration, services)
            return dynamicProperties
        }catch(Exception e){
            log.error("error dynamicProperties plugin ${serviceName}: ${e.message}")
        }

        return null
    }

    /**
     * load the dynamic select values for properties from the plugin, or null
     * @param serviceName
     * @param type
     * @param project
     * @param services
     * @return
     */
    def Map<String, Object> getDynamicDefaults(
        IFramework rundeckFramework,
        String serviceName,
        String type,
        String project,
        Services services
    ) {

        PropertyRetriever retriever = PropertyResolverFactory.instanceRetriever([:])
        PropertyRetriever projectRetriever = PropertyResolverFactory.instanceRetriever(rundeckFramework.getFrameworkProjectMgr().getFrameworkProject(project).getProperties())
        PropertyResolverFactory.Factory resolverFactory = PropertyResolverFactory.pluginPrefixedScoped(retriever,projectRetriever,rundeckFramework.getPropertyRetriever())
        def plugin = configurePluginWithoutValidation(type, createPluggableService(getPluginTypeByService(serviceName)), resolverFactory, PropertyScope.Instance, null)

        def instance=plugin?.instance
        if(!(instance instanceof DynamicProperties)){
            return null
        }

        try{
            return instance.dynamicDefaults(plugin.configuration, services)
        }catch(Exception e){
            log.error("error dynamicProperties plugin ${serviceName}: ${e.message}")
        }

        return null
    }

    public <T> PluggableProviderService<T> createPluggableService(Class<T> type) {
        if (rundeckPluginRegistry.isFrameworkDependentPluginType(type)) {
            return rundeckPluginRegistry.getFrameworkDependentPluggableService(
                    type,
                    (Framework) frameworkService.rundeckFramework
            )
        }
        if(type == StoragePlugin) return frameworkService.storageProviderPluginService
        rundeckPluginRegistry?.createPluggableService(type)
    }


    @Override
     <T> ConfiguredPlugin<T> configurePlugin(String name, Map<String,Object> configuration, PluggableProviderService<T> service) {
        def validation = rundeckPluginRegistry?.validatePluginByName(name, service, configuration)
        if (validation != null && !validation.valid) {
            logValidationErrors(service.name, name, validation.report )
            return null
        }
        def result = rundeckPluginRegistry?.configurePluginByName(name, service, configuration)
        if (result?.instance != null) {
            return result
        }
        log.error("${service.name} not found: ${name}")
        return null
    }

    @Override
    <T> ConfiguredPlugin<T> configurePlugin(String name, Map<String,Object> configuration, Class<T> type) {
        configurePlugin(name, configuration, createPluggableService(type))
    }

    @Override
    <T> ConfiguredPlugin<T> configurePlugin(String name, String service, Map<String,Object> configuration) {
        Class<?> serviceType = getPluginTypeByService(service)
        configurePlugin(name, configuration, rundeckPluginRegistry?.createPluggableService((Class<T>) serviceType))
    }

    @Override
    def <T> SimplePluginProviderLoader<T> createSimplePluginLoader(
            String projectName,
            IFramework framework,
            PluggableProviderService<T> service
    )
    {
        return { String provider, Map<String, Object> config ->
            def plugin = configurePlugin(provider, (Map) config, projectName, framework, service)
            plugin?.instance
        } as SimplePluginProviderLoader<T>
    }

    @Override
    <T> ConfiguredPlugin<T> configurePlugin(String name, Map<String,Object> configuration, String projectName,
                                                IFramework framework,
                                                PluggableProviderService<T> service) {
        def validation = rundeckPluginRegistry?.validatePluginByName(name, service, framework, projectName, configuration)
        if (!validation) {
            logValidationErrors(service.name, name, Validator.errorReport('provider', 'Not found: ' + name))
            return null
        }
        if (!validation.valid) {
            logValidationErrors(service.name, name, validation.report)
            return null
        }
        def result = rundeckPluginRegistry?.configurePluginByName(name, service, framework, projectName, configuration)
        if (result.instance != null) {
            return result
        }
        log.error("${service.name} not found: ${name}")
        return null
    }

    @Override
    <T> ConfiguredPlugin<T> configurePlugin(
            String name, Map<String,Object> configuration, String projectName,
            IFramework framework,
            Class<T> type
    )
    {
        configurePlugin(
                name,
                configuration,
                projectName,
                framework,
                createPluggableService(type)
        )
    }


    @Override
    <T> ConfiguredPlugin<T> configurePlugin(
            String name,
            PluggableProviderService<T> service,
            PropertyResolver resolver,
            PropertyScope defaultScope
    )
    {
        configurePlugin(name, service, resolver, defaultScope, null)
    }

    @Override
    <T> ConfiguredPlugin<T> configurePlugin(
            String name,
            PluggableProviderService<T> service,
            PropertyResolverFactory.Factory factory,
            PropertyScope defaultScope
    )
    {
        configurePlugin(name, service, factory, defaultScope, null)
    }


    @Override
    <T> ConfiguredPlugin<T> configurePlugin(
            String name,
            PluggableProviderService<T> service,
            PropertyResolver resolver,
            PropertyScope defaultScope,
            Services servicesProvider
    ) {
        return configurePlugin(
            name,
            service,
            PropertyResolverFactory.creates(resolver),
            defaultScope,
            servicesProvider
        )
    }

    @Override
    boolean hasRegisteredProvider(final String name, final Class type) {
        return rundeckPluginRegistry.hasRegisteredPlugin(createPluggableService(type).name, name)
    }

    @Override
    <T> ConfiguredPlugin<T> configurePlugin(
            String name,
            PluggableProviderService<T> service,
            PropertyResolverFactory.Factory resolverFactory,
            PropertyScope defaultScope,
            Services servicesProvider
    )
    {
        def validation = rundeckPluginRegistry?.validatePluginByName(name, service, resolverFactory, defaultScope)
        if(null==validation){
            return null
        }
        if(!validation.valid) {
            logValidationErrors(service.name, name, validation.report)
            return null
        }
        def result = rundeckPluginRegistry?.configurePluginByName(name, service, resolverFactory, defaultScope)

        if (result.instance != null) {
            serviceSpiProvider(result, servicesProvider)

            return result
        }
        log.error("${service.name} not found: ${name}")
        return null
    }

    <T> ConfiguredPlugin<T> configurePluginWithoutValidation(
            String name,
            PluggableProviderService<T> service,
            PropertyResolverFactory.Factory resolverFactory,
            PropertyScope defaultScope,
            Services servicesProvider
    )
    {

        def result = rundeckPluginRegistry?.configurePluginByName(name, service, resolverFactory, defaultScope)

        if (result?.instance != null) {
            serviceSpiProvider(result, servicesProvider)

            return result
        }
        log.error("${service.name} not found: ${name}")
        return null
    }

    def serviceSpiProvider(ConfiguredPlugin plugin, Services providers) {
        def instance = plugin.instance
        if(instance instanceof AcceptsServices){
            instance.setServices(providers)
        }
    }

    @Override
    <T> ConfiguredPlugin<T> configurePlugin(
            String name,
            Class<T> type,
            PropertyResolver resolver,
            PropertyScope defaultScope
    )
    {
        configurePlugin(name, createPluggableService(type), resolver, defaultScope)
    }
    /**
     * Return the configured values for a plugin
     * @param name provider name
     * @param service service
     * @param resolver property resolver for configuration properties
     * @param defaultScope default plugin property scope
     * @return Map of [instance: plugin instance, configuration: Map of resolved configuration properties], or null
     */
    def <T> Map getPluginConfiguration(String name, PluggableProviderService<T> service, PropertyResolverFactory.Factory factory, PropertyScope defaultScope) {
        return rundeckPluginRegistry?.getPluginConfigurationByName(
            name,
            service,
            factory,
            defaultScope
        )
    }

    private void logValidationErrors(String svcName, String pluginName,Validator.Report report) {
        def sb = new StringBuilder()
        sb<< "${svcName}: configuration was not valid for plugin '${pluginName}': "
        if(report?.errors){
            report?.errors?.each { k, v ->
                sb<<"${k}: ${v}\n"
            }
        }else{
            sb<<'(Unknown reason)'
        }
        log.error(sb.toString())
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
    def ValidatedPlugin validatePlugin(String name, PluggableProviderService service, PropertyResolverFactory.Factory factory, PropertyScope defaultScope, PropertyScope ignoredScope) {
        return rundeckPluginRegistry?.validatePluginByName(
            name,
            service,
            factory,
            defaultScope,
            ignoredScope
        )
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
    def ValidatedPlugin validatePlugin(String name, PluggableProviderService service, String project, Map config, PropertyScope defaultScope, PropertyScope ignoredScope) {
        return rundeckPluginRegistry?.validatePluginByName(
            name,
            service,
            frameworkService.pluginConfigFactory(project,config),
            defaultScope,
            ignoredScope
        )
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
        return rundeckPluginRegistry?.validatePluginByName(name, createPluggableService(clazz), config)
    }
    /**
     * Configure a new plugin using a specific property resolver for configuration
     * @param name provider name
     * @param clazz service class type
     * @param resolver project resolver
     * @param defaultScope default scope
     * @param config instance configuration data
     * @return validation
     */
    def ValidatedPlugin validatePluginConfig(
            String name,
            Class clazz,
            String project,
            Map config
    ) {
        def service = createPluggableService(clazz)
        return rundeckPluginRegistry?.validatePluginByName(
                name,
                service,
                frameworkService.rundeckFramework,
                project,
                config
        )
    }

    def <T> Map<String, DescribedPlugin<T>> listPlugins(Class<T> clazz) {
        listPlugins(clazz, createPluggableService(clazz))
    }

    /**
     * List all plugins with a valid Description
     * @param serviceName name of service
     * @return List of Description
     */
    def List<Description> listPluginDescriptions(String serviceName) {
        Class pluginClazz = ServiceTypes.getPluginType(serviceName)
        listPluginDescriptions(pluginClazz, createPluggableService(pluginClazz))
    }
    /**
     * List all plugins with a valid Description
     * @param clazz
     * @param service
     * @return List of Description
     */
    def <T> List<Description> listPluginDescriptions(Class<T> clazz, PluggableProviderService<T> service) {
        listPlugins(clazz, service).findAll { it.value.description }.collect { it.value.description }
    }
    /**
     * @param clazz
     * @param service
     * @return map of [name: DescribedPlugin]
     */
    def <T> Map<String, DescribedPlugin<T>> listPlugins(Class<T> clazz,PluggableProviderService<T> service) {
        return rundeckPluginRegistry?.listPluginDescriptors(clazz, service)
    }

    ResourceFormatParser getResourceFormatParser(String format) {
        def parser = getPlugin(format, ResourceFormatParser)
        if (!parser) {
            throw new UnsupportedFormatException("Unsupported format: " + format)
        }
        parser
    }

    ResourceFormatGenerator getResourceFormatGenerator(String format) {
        def generator = getPlugin(format, ResourceFormatGenerator)
        if (!generator) {
            throw new UnsupportedFormatException("Unsupported format: " + format)
        }
        generator
    }

    PluginRegistry getPluginRegistry() {
        return rundeckPluginRegistry
    }
}
