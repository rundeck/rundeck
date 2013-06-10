package com.dtolabs.rundeck.server.plugins

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.plugins.PluggableProviderService
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolver
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope

/**
 * Interface for getting and configuring plugins
 */
public interface PluginRegistry {

    /**
     * Create and configure a plugin instance with the given bean or provider name
     * @param name name of bean or provider
     * @param service provider service
     * @param configuration map of configuration data
     * @return
     */
    Object configurePluginByName(String name, PluggableProviderService service, Map configuration);
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
    Object configurePluginByName(String name, PluggableProviderService service, Framework framework,
                                        String project, Map instanceConfiguration) ;
    /**
     * Create and configure a plugin instance with the given bean or provider name using a property resolver and a
     * default property scope
     * @param name name of bean or provider
     * @param service provider service
     * @param resolver a property resolver
     * @param defaultScope default scope to search for property values when undeclared
     * @return
     */
    Object configurePluginByName(String name, PluggableProviderService service, PropertyResolver resolver, PropertyScope defaultScope) ;
    /**
     * Create and configure a plugin instance with the given bean or provider name using a property resolver and a
     * default property scope
     * @param name name of bean or provider
     * @param service provider service
     * @param resolver a property resolver
     * @param defaultScope default scope to search for property values when undeclared
     * @return
     */
    Map validatePluginByName(String name, PluggableProviderService service, PropertyResolver resolver, PropertyScope defaultScope) ;
    /**
     * Load a plugin instance with the given bean or provider name
     * @param name name of bean or provider
     * @param service provider service
     * @return
     */
    Object loadPluginByName(String name, PluggableProviderService service) ;
    /**
     * Load a plugin instance with the given bean or provider name
     * @param name name of bean or provider
     * @param service provider service
     * @return map containing [instance:(plugin instance), description: (map or Description),
     */
    Map loadPluginDescriptorByName(String name, PluggableProviderService service) ;

    /**
     * List all plugin type definitions that are either ServiceProvider plugins of the given service name,
     * or are groovy plugins of the given type
     * @param providerServiceName
     * @param groovyPluginType
     * @return
     */
    Map<String, Object> listPlugins(Class groovyPluginType, PluggableProviderService service);
    /**
     * List all plugin type definitions that are either ServiceProvider plugins of the given service name,
     * or are groovy plugins of the given type
     * @param providerServiceName
     * @param groovyPluginType
     * @return
     */
    Map<String, Object> listPluginDescriptors(Class groovyPluginType, PluggableProviderService service) ;
}
