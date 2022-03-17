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

package com.dtolabs.rundeck.server.plugins

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.IFramework
import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException
import com.dtolabs.rundeck.core.execution.service.MissingProviderException
import com.dtolabs.rundeck.core.execution.service.ProviderLoaderException
import com.dtolabs.rundeck.core.plugins.CloseableProvider
import com.dtolabs.rundeck.core.plugins.ConfiguredPlugin
import com.dtolabs.rundeck.core.plugins.DescribedPlugin
import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.plugins.PluginMetadata
import com.dtolabs.rundeck.core.plugins.PluginRegistry
import com.dtolabs.rundeck.core.plugins.PluginResourceLoader
import com.dtolabs.rundeck.core.plugins.ValidatedPlugin
import com.dtolabs.rundeck.core.plugins.configuration.DescribableServiceUtil
import com.dtolabs.rundeck.core.plugins.configuration.PluginAdapterUtility
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolver
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolverFactory
import com.dtolabs.rundeck.core.plugins.PluggableProviderService
import com.dtolabs.rundeck.core.plugins.ProviderIdent
import com.dtolabs.rundeck.core.plugins.ServiceProviderLoader
import com.dtolabs.rundeck.core.plugins.configuration.Describable
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.core.plugins.configuration.Validator
import com.dtolabs.rundeck.plugins.CorePluginProviderServices
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.ServiceTypes
import com.dtolabs.rundeck.plugins.config.ConfiguredBy
import com.dtolabs.rundeck.plugins.config.PluginGroup
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder
import com.dtolabs.rundeck.server.plugins.services.PluginBuilder
import groovy.transform.PackageScope
import org.rundeck.security.RundeckPluginBlocklist
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.BeanNotOfRequiredTypeException
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

/**
 * Manages a registry for two kinds of plugins: groovy plugins loaded via spring,
 * and Rundeck-core style jar/zip plugins, loaded via {@link ServiceProviderLoader}
 * User: greg
 * Date: 4/11/13
 * Time: 7:07 PM
 */
class RundeckPluginRegistry implements ApplicationContextAware, PluginRegistry, InitializingBean {
    public static Logger log = LoggerFactory.getLogger(RundeckPluginRegistry.class.name)
    /**
     * Registry of spring bean plugin providers, "providername"->"beanname"
     */
    HashMap pluginRegistryMap
    def ApplicationContext applicationContext
    /**
     * groovy plugin sources loaded dynamically will live in a sub context
     */
    def Map<String,ApplicationContext> subContexts=[:]
    def ServiceProviderLoader rundeckServerServiceProviderLoader
    def File pluginDirectory
    def File pluginCacheDirectory
    def RundeckEmbeddedPluginExtractor rundeckEmbeddedPluginExtractor
    RundeckPluginBlocklist rundeckPluginBlocklist

    @Override
    void afterPropertiesSet() throws Exception {
        rundeckEmbeddedPluginExtractor.rundeckPluginRegistry = this
        def result = rundeckEmbeddedPluginExtractor.extractEmbeddedPlugins()
        if (!result.success) {
            log.error("Failed extracting embedded plugins: " + result.message)
            result?.errors?.each {
                log.error(it)
            }
        }
        result?.logs?.each {
            log.debug(it)
        }
    }

    void registerPlugin(String type, String name, String beanName) {
        pluginRegistryMap.putIfAbsent(type + ":" + name, beanName)
    }

    String createServiceName(final String simpleName) {
        if (simpleName.endsWith("Plugin")) {
            return simpleName.substring(0, simpleName.length() - "Plugin".length());
        }
        return simpleName;
    }

    public <T> boolean isFrameworkDependentPluginType(Class<T> type) {
        return CorePluginProviderServices.isFrameworkDependentPluginType(type)
    }

    @Override
    def <T> PluggableProviderService<T> getFrameworkDependentPluggableService(
            final Class<T> type,
            final Framework framework
    ) {
        return CorePluginProviderServices.getPluggableProviderServiceForType(type,framework)
    }

    public <T> PluggableProviderService<T> createPluggableService(Class<T> type) {
        String found = ServiceTypes.pluginTypesMap.find { it.value == type }?.key
        def name = found ?: createServiceName(type.getSimpleName())
        rundeckServerServiceProviderLoader.createPluginService(type, name)
    }
    /**
     * Create and configure a plugin instance with the given bean or provider name
     * @param name name of bean or provider
     * @param service provider service
     * @param configuration map of configuration data
     * @return
     */
    public <T> ConfiguredPlugin<T> configurePluginByName(String name, PluggableProviderService<T> service, Map configuration) {
        final PropertyResolver resolver = PropertyResolverFactory.createInstanceResolver(configuration);
        return configurePluginByName(name, service, resolver, PropertyScope.InstanceOnly)
    }


    /**
     * Create and configure a plugin instance with the given bean or provider name, resolving properties via
     * the framework and specified project properties as well as instance configuration.
     * @param name name of bean or provider
     * @param service provider service
     * @param framework framework
     * @param project project name or null
     * @param instanceConfiguration configuration or null
     * @return
     */
    public <T> ConfiguredPlugin<T> configurePluginByName(
            String name,
            PluggableProviderService<T> service,
            IFramework framework,
            String project, Map instanceConfiguration
    )
    {
        return configurePluginByName(
            name,
            service,
            PropertyResolverFactory
                .createFrameworkProjectRuntimeResolverFactory(framework, project, instanceConfiguration),
            PropertyScope.Instance
        )
    }

    /**
     * Create and configure a plugin instance with the given bean or provider name using a property resolver and a
     * default property scope
     * @param name name of bean or provider
     * @param service provider service
     * @param resolver a property resolver
     * @param defaultScope default scope to search for property values when undeclared
     * @return Map of [instance: plugin instance, configuration: resolved configuration properties]
     */
    public <T> ConfiguredPlugin<T> configurePluginByName(String name, PluggableProviderService<T> service,
    PropertyResolver resolver, PropertyScope defaultScope) {
        return configurePluginByName(name, service, PropertyResolverFactory.creates(resolver), defaultScope)
    }

    /**
     * Create and configure a plugin instance with the given bean or provider name using a property resolver and a
     * default property scope
     * @param name name of bean or provider
     * @param service provider service
     * @param resolver a property resolver
     * @param defaultScope default scope to search for property values when undeclared
     * @return Map of [instance: plugin instance, configuration: resolved configuration properties]
     */
    public <T> ConfiguredPlugin<T> configurePluginByName(
        String name,
        PluggableProviderService<T> service,
        PropertyResolverFactory.Factory resolverFactory,
        PropertyScope defaultScope
    ) {
        DescribedPlugin<T> pluginDesc = loadPluginDescriptorByName(name, service)
        if(null==pluginDesc){
            return null
        }
        T plugin = pluginDesc.instance
        def description = pluginDesc.description
        Map<String, Object> config=null
        if (description) {
            config = PluginAdapterUtility.configureProperties(resolverFactory.create(service.name,name), description, plugin, defaultScope);
        }
        if(plugin instanceof ConfiguredBy && pluginDesc.groupDescribedPlugin) {
            def grouped = configurePluginByName(
                pluginDesc.groupDescribedPlugin.name,
                createPluggableService(PluginGroup),
                resolverFactory,
                defaultScope
            )
            if(grouped.instance){
                ((ConfiguredBy)plugin).pluginGroup=grouped.instance
            }
        }
        new ConfiguredPlugin<T>(plugin, config)
    }
    /**
     * Create and configure a plugin instance with the given bean or provider name using a property resolver and a
     * default property scope, retain the instance to prevent unloading it
     * @param name name of bean or provider
     * @param service provider service
     * @param resolver a property resolver
     * @param defaultScope default scope to search for property values when undeclared
     * @return ConfiguredPlugin with a closeable reference to release the plugin
     */
    public <T> ConfiguredPlugin<T> retainConfigurePluginByName(
            String name, PluggableProviderService<T> service,
            PropertyResolver resolver,
            PropertyScope defaultScope
    )
    {
        return retainConfigurePluginByName(
            name,
            service,
            PropertyResolverFactory.creates(resolver),
            defaultScope
        )
    }
    /**
     * Create and configure a plugin instance with the given bean or provider name using a property resolver and a
     * default property scope, retain the instance to prevent unloading it
     * @param name name of bean or provider
     * @param service provider service
     * @param resolver a property resolver
     * @param defaultScope default scope to search for property values when undeclared
     * @return ConfiguredPlugin with a closeable reference to release the plugin
     */
    public <T> ConfiguredPlugin<T> retainConfigurePluginByName(
            String name,
            PluggableProviderService<T> service,
            PropertyResolverFactory.Factory resolverFactory,
            PropertyScope defaultScope
    )
    {
        CloseableDescribedPlugin<T> pluginDesc = retainPluginDescriptorByName(name, service)
        if (null == pluginDesc) {
            return null
        }
        T plugin = pluginDesc.instance
        def description = pluginDesc.description
        Map<String, Object> config = null
        if (description) {
            config = PluginAdapterUtility.configureProperties(resolverFactory.create(service.name,name), description, plugin, defaultScope);
        }
        if(plugin instanceof ConfiguredBy && pluginDesc.groupDescribedPlugin) {
            def grouped = configurePluginByName(
                pluginDesc.groupDescribedPlugin.name,
                createPluggableService(PluginGroup),
                resolverFactory,
                defaultScope
            )
            if(grouped.instance){
                ((ConfiguredBy)plugin).pluginGroup=grouped.instance
            }
        }
        new ConfiguredPlugin<T>(plugin, config, pluginDesc.closeable)
    }

    public <T> Map<String,Object> getPluginConfigurationByName(String name, PluggableProviderService<T> service,
                                               PropertyResolver resolver, PropertyScope defaultScope) {
        getPluginConfigurationByName(name, service, PropertyResolverFactory.creates(resolver), defaultScope)
    }

    public <T> Map<String,Object> getPluginConfigurationByName(String name, PluggableProviderService<T> service,
                                               PropertyResolverFactory.Factory factory, PropertyScope defaultScope) {
        DescribedPlugin<T> pluginDesc = loadPluginDescriptorByName(name, service)
        if (null == pluginDesc) {
            return null
        }
        def description = pluginDesc.description
        Map<String, Object> config=[:]
        if (description && description instanceof Description) {
            config = PluginAdapterUtility.mapDescribedProperties(factory.create(service.name,name), description, defaultScope)
        }
        return config
    }
    /**
     *
     * Validate a provider for a service using the framework, project name and instance configuration map
     * @param name name of bean or provider
     * @param service provider service
     * @param framework the framework
     * @param project the project name
     * @param instanceConfiguration config map
     * @return Map containing valid:true/false, and report: {@link Validator.Report}
     */
    public ValidatedPlugin validatePluginByName(
            String name, PluggableProviderService service,
            IFramework framework,
            String project, Map instanceConfiguration
    )
    {
        final PropertyResolver resolver = PropertyResolverFactory.createFrameworkProjectRuntimeResolver(framework,
                project, instanceConfiguration, service.getName(), name);
        return validatePluginByName(name, service, resolver, PropertyScope.Instance)
    }

    /**
     * Validate a provider for a service with an instance configuration
     * @param name name of bean or provider
     * @param service provider service
     * @param instanceConfiguration config map
     * @return Map containing valid:true/false, and report: {@link Validator.Report}
     */
    ValidatedPlugin validatePluginByName(String name, PluggableProviderService service, Map instanceConfiguration) {
        final PropertyResolver resolver = PropertyResolverFactory.createInstanceResolver(instanceConfiguration);
        return validatePluginByName(name, service, resolver, PropertyScope.InstanceOnly)
    }
    /**
     * Validate a provider for a service with an instance configuration
     * @param name name of bean or provider
     * @param service provider service
     * @param instanceConfiguration config map
     * @return Map containing valid:true/false, and report: {@link Validator.Report}
     */
    ValidatedPlugin validatePluginByName(String name, PluggableProviderService service, Map instanceConfiguration, PropertyScope ignoredScope) {
        final PropertyResolver resolver = PropertyResolverFactory.createInstanceResolver(instanceConfiguration);
        return validatePluginByName(name, service, resolver, PropertyScope.InstanceOnly, ignoredScope)
    }
    /**
     * Validate a provider for a service using a property resolver and a
     * default property scope
     * @param name name of bean or provider
     * @param service provider service
     * @param resolver a property resolver
     * @param defaultScope default scope to search for property values when undeclared
     * @return Map containing valid:true/false, and report: {@link Validator.Report}
     */
    public ValidatedPlugin validatePluginByName(String name, PluggableProviderService service,
                                                PropertyResolver resolver,
                                                PropertyScope defaultScope) {
        return validatePluginByName(name, service, PropertyResolverFactory.creates(resolver), defaultScope)
    }
    /**
     * Validate a provider for a service using a property resolver and a
     * default property scope
     * @param name name of bean or provider
     * @param service provider service
     * @param resolver a property resolver
     * @param defaultScope default scope to search for property values when undeclared
     * @return Map containing valid:true/false, and report: {@link Validator.Report}
     */
    public ValidatedPlugin validatePluginByName(String name, PluggableProviderService service,
                                                PropertyResolverFactory.Factory resolverFactory,
                                                PropertyScope defaultScope) {
        return validatePluginByName(name, service, resolverFactory, defaultScope, null)
    }
    /**
     * Validate a provider for a service using a property resolver and a
     * default property scope, and an ignoredScope
     * @param name name of bean or provider
     * @param service provider service
     * @param resolver a property resolver
     * @param defaultScope default scope to search for property values when undeclared
     * @return Map containing valid:true/false, and report: {@link Validator.Report}
     */
    public ValidatedPlugin validatePluginByName(
            String name,
            PluggableProviderService service,
            PropertyResolver resolver,
            PropertyScope defaultScope, PropertyScope ignoredScope
    )
    {
        return validatePluginByName(
            name,
            service,
            PropertyResolverFactory.creates(resolver),
            defaultScope,
            ignoredScope
        )
    }
    /**
     * Validate a provider for a service using a property resolver and a
     * default property scope, and an ignoredScope
     * @param name name of bean or provider
     * @param service provider service
     * @param resolver a property resolver
     * @param defaultScope default scope to search for property values when undeclared
     * @return Map containing valid:true/false, and report: {@link Validator.Report}
     */
    public ValidatedPlugin validatePluginByName(
            String name,
            PluggableProviderService service,
            PropertyResolverFactory.Factory resolverFactory,
            PropertyScope defaultScope,
            PropertyScope ignoredScope
    )
    {
        def pluginDesc = loadPluginDescriptorByName(name, service)
        if(null==pluginDesc) {
            return null
        }
        ValidatedPlugin result = new ValidatedPlugin()
        def description = pluginDesc.description
        if (description && description instanceof Description) {
            def report = Validator.validate(resolverFactory.create(service.name,name), description, defaultScope, ignoredScope)
            result.valid = report.valid
            result.report = report
        }
        result
    }
    /**
     * Load a plugin instance with the given bean or provider name
     * @param name name of bean or provider
     * @param service provider service
     * @return
     */
    public <T> T loadPluginByName(String name, PluggableProviderService<T> service) {
        return loadPluginDescriptorByName(name, service)?.instance
    }

    @Override
    def <T> CloseableProvider<T> retainPluginByName(final String name, final PluggableProviderService<T> service) {
        return retainPluginDescriptorByName(name, service)?.closeable
    }

    /**
     * Load a closeable plugin instance with the given bean or provider name,
     * should be used when the plugin instance will be retained for use over a period of time
     * @param name name of bean or provider
     * @param service provider service
     * @return CloseableDescribedPlugin , or null if it cannot be loaded
     */
    public <T> CloseableDescribedPlugin<T> retainPluginDescriptorByName(
            String name,
            PluggableProviderService<T> service
    )
    {
        DescribedPlugin<T> beanPlugin = loadBeanDescriptor(name, service.name)
        if (null != beanPlugin) {
            return new CloseableDescribedPlugin<T>(beanPlugin)
        }

        if(rundeckPluginBlocklist.isPluginProviderPresent(service.name, name)){
            return null
        }
        //try loading via ServiceProviderLoader
        if (rundeckServerServiceProviderLoader && service) {
            //attempt to load directly
            CloseableProvider<T> instance
            try {
                instance = service.closeableProviderOfType(name);
            } catch (ExecutionServiceException ignored) {
                //not already loaded, attempt to load...
            }
            if (null == instance) {
                try {
                    instance = rundeckServerServiceProviderLoader.loadCloseableProvider(service, name)
                } catch (MissingProviderException exception) {
                    log.error("Plugin ${name} for service: ${service.name} was not found")
                    log.debug("Plugin ${name} for service: ${service.name} was not found", exception)
                } catch (ProviderLoaderException exception) {
                    log.error("Failure loading Rundeck plugin: ${name} for service: : ${service.name}", exception)
                }
            }
            if (null != instance) {
                def d = loadPluginDescription(service, name)
                return new CloseableDescribedPlugin<T>(instance, d, name,null,loadGroupDescribedPlugin(d, service.name))
            }
        }
        null
    }
    /**
     * Load a plugin instance with the given bean or provider name
     * @param name name of bean or provider
     * @param service provider service
     * @return DescribedPlugin, or null if it cannot be loaded
     */
    public <T> DescribedPlugin<T> loadPluginDescriptorByName(String name, PluggableProviderService<T> service) {
         DescribedPlugin<T> beanPlugin = loadBeanDescriptor(name, service.name)
        if (null != beanPlugin) {
            return beanPlugin
        }

        if(rundeckPluginBlocklist.isPluginProviderPresent(service.name, name)){
            return null
        }
        //try loading via ServiceProviderLoader
        if (rundeckServerServiceProviderLoader && service) {
            //attempt to load directly
            T instance
            try {
                instance = service.providerOfType(name);
            } catch (ExecutionServiceException ignored) {
                //not already loaded, attempt to load...
            }
            if (null == instance) {
                try {
                    instance = rundeckServerServiceProviderLoader.loadProvider(service, name)
                } catch (MissingProviderException exception) {
                    log.error("Plugin ${name} for service: ${service.name} was not found")
                    log.debug("Plugin ${name} for service: ${service.name} was not found", exception)
                } catch (ProviderLoaderException exception) {
                    log.error("Failure loading Rundeck plugin: ${name} for service: : ${service.name}", exception)
                }
            }
            if (null != instance) {
                def d = loadPluginDescription(service, name)
                return new DescribedPlugin<T>(instance, d, name, null, loadGroupDescribedPlugin(d, service.name))
            }
        }
        null
    }

    /**
     * Load described plugin for plugin group
     * @param d description
     * @param serviceName service name
     * @return described plugin, or null if no group
     */
    private DescribedPlugin<PluginGroup> loadGroupDescribedPlugin(Description d, String serviceName) {
        DescribedPlugin groupInstance = null
        if (d.pluginGroupType
            //don't allow nested groups
            && serviceName != ServiceNameConstants.PluginGroup
        ) {
            //load group instance
            Description groupTypeDesc = PluginAdapterUtility.buildDescription(d.pluginGroupType,DescriptionBuilder.builder())
            String groupTypeName = groupTypeDesc.name
            groupInstance = loadPluginDescriptorByName(groupTypeName, createPluggableService(PluginGroup))
        }
        groupInstance
    }

    private Description loadPluginDescription(PluggableProviderService service, String name){
        return DescribableServiceUtil.loadDescriptionForType(service, name, true)
    }

    private <T> DescribedPlugin<T> loadBeanDescriptor(String name, String type = null) {
        try {
            def beanName = pluginRegistryMap["${type}:${name}"] ?: pluginRegistryMap[name]
            if (beanName) {
                def bean = findBean(beanName)
                if(!bean){
                    return null
                }
                if (bean instanceof PluginBuilder) {
                    bean = ((PluginBuilder) bean).buildPlugin()
                }
                if(!bean){
                    return null
                }

                final Plugin annotation1 = bean.getClass().getAnnotation(Plugin.class);
                if (type && annotation1 && annotation1.service() != type) {
                    return null
                }

                Description desc = null
                if (bean instanceof Describable) {
                    desc = ((Describable) bean).description
                } else if (PluginAdapterUtility.canBuildDescription(bean)) {
                    desc = PluginAdapterUtility.buildDescription(bean, DescriptionBuilder.builder())
                }
                if(!desc){
                    return null
                }
                return new DescribedPlugin<T>((T)bean, desc, name, new File(pluginDirectory, name + ".groovy"),null)
            }
        } catch (NoSuchBeanDefinitionException e) {
            log.error("plugin Spring bean does not exist: ${name}")
        }
        return null
    }

    def registerDynamicPluginBean(String beanName, ApplicationContext context){
        subContexts[beanName]=context
        pluginRegistryMap[beanName]=beanName
    }
    /**
     * Look for specified bean in subcontexts if present, or in applicationContext
     * @param beanName
     * @return
     */
    @PackageScope
    Object findBean(String beanName) {
        (subContexts[beanName]?:applicationContext).getBean(beanName)
    }

    /**
     * List all plugin type definitions that are either ServiceProvider plugins of the given service name,
     * or are groovy plugins of the given type
     * @param providerServiceName
     * @param groovyPluginType
     * @return
     */
    public Map<String, Object> listPlugins(Class groovyPluginType, PluggableProviderService service) {
        def list = listPluginDescriptors(groovyPluginType, service)
        def Map map = [:]
        list.each { k, v ->
            map[k] = v['instance']
        }
        map
    }
    /**
     * List all plugin type definitions that are either ServiceProvider plugins of the given service name,
     * or are groovy plugins of the given type
     * @param providerServiceName
     * @param groovyPluginType
     * @return
     */
    public <T> Map<String, DescribedPlugin<T>> listPluginDescriptors(Class groovyPluginType,
                                                                   PluggableProviderService<T> service) {
        def Map<String,DescribedPlugin<T>> list= [:]
        pluginRegistryMap.each { String k, String v ->
            try {
                String pluginName = extractPluginName(k)
                if(rundeckPluginBlocklist.isPluginProviderPresent(service.name, pluginName)){
                    return
                }
                def bean = findBean(v)
                if (bean instanceof PluginBuilder) {
                    bean = ((PluginBuilder) bean).buildPlugin()
                }
                if (bean != null && groovyPluginType.isAssignableFrom(bean.class)) {
                    def file = new File(pluginDirectory, pluginName + ".groovy")
                    //try to check annotations
                    Description desc=null
                    if (bean instanceof Describable) {
                        desc = ((Describable) bean).description
                    } else if (PluginAdapterUtility.canBuildDescription(bean)) {
                        desc = PluginAdapterUtility.buildDescription(bean, DescriptionBuilder.builder())
                    }

                    list[pluginName] = new DescribedPlugin(
                        bean,
                        desc,
                        pluginName,
                        file,
                        loadGroupDescribedPlugin(desc, service.name)
                    )
                }
            } catch (NoSuchBeanDefinitionException e) {
                log.error("No such bean: ${v}")
            } catch (BeanNotOfRequiredTypeException e) {
                log.error("Not a valid NotificationPlugin: ${v}")
            }
        }
        if (rundeckServerServiceProviderLoader && service) {
            service.listProviders().each { ProviderIdent ident ->
                if(rundeckPluginBlocklist.isPluginProviderPresent(service.name, ident.providerName)){
                    return
                }
                def instance
                try {
                    instance= service.providerOfType(ident.providerName)
                } catch (ExecutionServiceException ignored) {
                }
                if(null==instance){
                    instance = rundeckServerServiceProviderLoader.loadProvider(service, ident.providerName)
                }
                if (!groovyPluginType.isAssignableFrom(instance.class)) {
                    return
                }

                if (!list[ident.providerName]) {
                    list[ident.providerName] = new DescribedPlugin<T>(instance, null, ident.providerName,null,null)
                }
            }
            service.listDescriptions()?.each { Description d ->
                if(rundeckPluginBlocklist.isPluginProviderPresent(service.name, d.name)){
                    return
                }
                if (!list[d.name]) {
                    list[d.name] = new DescribedPlugin<T>( null, null, d.name,null,null)
                }
                list[d.name].description = d
                list[d.name].groupDescribedPlugin = loadGroupDescribedPlugin(d, service.name)
            }
        }

        list
    }

    private String extractPluginName(String key){
        List k = key?.split(':')
        if(k?.size() > 1) {
            return k.get(1)
        }

        return key
    }

    @Override
    PluginResourceLoader getResourceLoader(String service, String provider) throws ProviderLoaderException {

        // check groovy bean plugins
        if (pluginRegistryMap["${service}:${provider}"] || pluginRegistryMap[provider]) {
            String beanName = pluginRegistryMap["${service}:${provider}"] ?: pluginRegistryMap[provider]
            try {
                def bean = findBean(beanName)
                if (bean instanceof PluginResourceLoader) {
                    return bean
                }
            } catch (NoSuchBeanDefinitionException e) {
                log.error("No such bean: ${beanName}")
            }
        }

        rundeckServerServiceProviderLoader.getResourceLoader service, provider
    }

    @Override
    PluginMetadata getPluginMetadata(final String service, final String provider) throws ProviderLoaderException {
        if (pluginRegistryMap["${service}:${provider}"] || pluginRegistryMap[provider]) {
            Class groovyPluginType = ServiceTypes.getPluginType(service)
            String beanName=pluginRegistryMap["${service}:${provider}"] ?: pluginRegistryMap[provider]
            try {
                def bean = findBean(beanName)
                if (bean instanceof PluginBuilder) {
                    if (bean.pluginClass == groovyPluginType) {
                        if (bean instanceof PluginMetadata) {
                            def metadata = bean as PluginMetadata
                            return metadata
                        }
                    }
                }
            } catch (NoSuchBeanDefinitionException e) {
                log.error("No such bean: ${beanName}")
            }
        }
        rundeckServerServiceProviderLoader.getPluginMetadata service, provider
    }
}
