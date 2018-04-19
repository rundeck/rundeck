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
import com.dtolabs.rundeck.core.execution.service.ProviderLoaderException
import com.dtolabs.rundeck.core.plugins.CloseableProvider
import com.dtolabs.rundeck.core.plugins.PluggableProviderService
import com.dtolabs.rundeck.core.plugins.PluginMetadata
import com.dtolabs.rundeck.core.plugins.PluginResourceLoader
import com.dtolabs.rundeck.core.plugins.ProviderIdent
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolver
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.core.utils.IPropertyLookup

import static com.dtolabs.rundeck.core.plugins.configuration.Validator.*

/**
 * Interface for getting and configuring plugins
 */
public interface PluginRegistry {

    public <T> PluggableProviderService<T> createPluggableService(Class<T> type);

    /**
     * Create and configure a plugin instance with the given bean or provider name
     * @param name name of bean or provider
     * @param service provider service
     * @param configuration map of configuration data
     * @return Map of [instance: plugin instance, configuration: resolved configuration properties]
     */
    public <T> ConfiguredPlugin<T> configurePluginByName(String name, PluggableProviderService<T> service, Map configuration);
    /**
     * Create and configure a plugin instance with the given bean or provider name, resolving properties via
     * the framework and specified project properties as well as instance configuration.
     * @param name name of bean or provider
     * @param service provider service
     * @param framework framework
     * @param project project name or null
     * @param instanceConfiguration configuration or null
     * @return Map of [instance: plugin instance, configuration: resolved configuration properties]
     */
    public <T> ConfiguredPlugin<T> configurePluginByName(String name, PluggableProviderService<T> service,
                                                     Framework framework,
                                        String project, Map instanceConfiguration) ;
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
            IPropertyLookup frameworkLookup,
            IPropertyLookup projectLookup,
            Map instanceConfiguration
    )

    public <T> ConfiguredPlugin<T> configurePluginByName(String name, PluggableProviderService<T> service,
                                                         PropertyResolver resolver, PropertyScope defaultScope);
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
            PropertyResolver resolver, PropertyScope defaultScope
    )
    /**
     * Return the mapped configuration properties for the plugin
     * @param name name of bean or provider
     * @param service provider service
     * @param resolver a property resolver
     * @param defaultScope default scope to search for property values when undeclared
     * @return Map of [instance: plugin instance, configuration: resolved configuration properties]
     */
    public <T> Map<String,Object> getPluginConfigurationByName(String name, PluggableProviderService<T> service,
                                              PropertyResolver resolver, PropertyScope defaultScope) ;
    /**
     * Validate a provider for a service using a property resolver and a
     * default property scope
     * @param name name of bean or provider
     * @param service provider service
     * @param resolver a property resolver
     * @param defaultScope default scope to search for property values when undeclared
     * @return Map containing valid:true/false, and report: {@link com.dtolabs.rundeck.core.plugins.configuration.Validator.Report}
     */
    ValidatedPlugin validatePluginByName(String name, PluggableProviderService service, PropertyResolver resolver, PropertyScope defaultScope) ;
    /**
     * Validate a provider for a service using a property resolver and a
     * default property scope, and an ignoredScope
     * @param name name of bean or provider
     * @param service provider service
     * @param resolver a property resolver
     * @param defaultScope default scope to search for property values when undeclared
     * @return Map containing valid:true/false, and report: {@link com.dtolabs.rundeck.core.plugins.configuration.Validator.Report}
     */
    ValidatedPlugin validatePluginByName(String name, PluggableProviderService service, PropertyResolver resolver,
                           PropertyScope defaultScope, PropertyScope ignoredScope) ;

    /**
     *
     * Validate a provider for a service using the framework, project name and instance configuration map
     * @param name name of bean or provider
     * @param service provider service
     * @param framework the framework
     * @param project the project name
     * @param instanceConfiguration config map
     * @return Map containing valid:true/false, and report: {@link com.dtolabs.rundeck.core.plugins.configuration.Validator.Report}
     */
    ValidatedPlugin validatePluginByName(String name, PluggableProviderService service, Framework framework,
                                    String project, Map instanceConfiguration);

    /**
     * Validate a provider for a service with an instance configuration
     * @param name name of bean or provider
     * @param service provider service
     * @param instanceConfiguration config map
     * @return Map containing valid:true/false, and report: {@link com.dtolabs.rundeck.core.plugins.configuration.Validator.Report}
     */
    ValidatedPlugin validatePluginByName(String name, PluggableProviderService service, Map instanceConfiguration);
    /**
     * Load a plugin instance with the given bean or provider name
     * @param name name of bean or provider
     * @param service provider service
     * @return
     */
    public <T> T loadPluginByName(String name, PluggableProviderService<T> service) ;

    public <T> CloseableProvider<T> retainPluginByName(String name, PluggableProviderService<T> service);
    /**
     * Load a plugin instance with the given bean or provider name
     * @param name name of bean or provider
     * @param service provider service
     * @return map containing [instance:(plugin instance), description: (map or Description),
     */
    public <T> DescribedPlugin<T> loadPluginDescriptorByName(String name, PluggableProviderService<T> service) ;

    /**
     * List all plugin type definitions that are either ServiceProvider plugins of the given service name,
     * or are groovy plugins of the given type
     * @param providerServiceName
     * @param groovyPluginType
     * @return
     */
    public <T> Map<String, Object> listPlugins(Class groovyPluginType, PluggableProviderService<T> service);
    /**
     * List all plugin type definitions that are either ServiceProvider plugins of the given service name,
     * or are groovy plugins of the given type
     * @param providerServiceName
     * @param groovyPluginType
     * @return
     */
    public <T> Map<String, DescribedPlugin<T>> listPluginDescriptors(Class groovyPluginType, PluggableProviderService<T> service) ;

    /**
     * Return plugin resource loader
     * @param service
     * @param provider
     * @return
     * @throws ProviderLoaderException
     */
    public PluginResourceLoader getResourceLoader(String service, String provider) throws ProviderLoaderException;

    /**
     * Return plugin metadata
     * @param service
     * @param provider
     * @return
     * @throws ProviderLoaderException
     */
    public PluginMetadata getPluginMetadata(String service, String provider) throws ProviderLoaderException;
}
