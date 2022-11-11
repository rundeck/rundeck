package com.dtolabs.rundeck.core.plugins;

import com.dtolabs.rundeck.core.common.IFramework;
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolver;
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolverFactory.Factory;
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope;
import org.rundeck.app.spi.Services;

import java.util.Map;

/**
 * interface to provide a configured plugin in various ways
 */
public interface PluginConfigureService {

    boolean hasRegisteredProvider(String name, Class<?> type);

    <T> ConfiguredPlugin<T> configurePlugin(
            String name, Class<T> type, PropertyResolver resolver, PropertyScope defaultScope
    );

    /**
     * Configure a new plugin using a specific property resolver for configuration
     *
     * @param name            provider name
     * @param service         service
     * @param resolverFactory property resolverfactory for configuration properties
     * @param defaultScope    default plugin property scope
     * @return Map of [instance: plugin instance, configuration: Map of resolved configuration properties], or null
     */
    <T> ConfiguredPlugin<T> configurePlugin(
            String name,
            PluggableProviderService<T> service,
            Factory resolverFactory,
            PropertyScope defaultScope,
            Services servicesProvider
    );

    /**
     * Configure a new plugin using a specific property resolver for configuration
     *
     * @param name         provider name
     * @param service      service
     * @param resolver     property resolver for configuration properties
     * @param defaultScope default plugin property scope
     * @return Map of [instance: plugin instance, configuration: Map of resolved configuration properties], or null
     */
    <T> ConfiguredPlugin<T> configurePlugin(
            String name,
            PluggableProviderService<T> service,
            PropertyResolver resolver,
            PropertyScope defaultScope,
            Services servicesProvider
    );

    /**
     * Configure a new plugin using a specific property resolver for configuration
     *
     * @param name         provider name
     * @param service      service
     * @param factory      property resolver for configuration properties
     * @param defaultScope default plugin property scope
     * @return Map of [instance: plugin instance, configuration: Map of resolved configuration properties], or null
     */
    <T> ConfiguredPlugin<T> configurePlugin(
            String name, PluggableProviderService<T> service, Factory factory, PropertyScope defaultScope
    );

    /**
     * Configure a new plugin using a specific property resolver for configuration
     *
     * @param name         provider name
     * @param service      service
     * @param resolver     property resolver for configuration properties
     * @param defaultScope default plugin property scope
     * @return Map of [instance: plugin instance, configuration: Map of resolved configuration properties], or null
     */
    <T> ConfiguredPlugin<T> configurePlugin(
            String name, PluggableProviderService<T> service, PropertyResolver resolver, PropertyScope defaultScope
    );

    /**
     * Configure a new plugin using only instance-scope configuration values
     *
     * @param name          provider name
     * @param configuration map of instance configuration values
     * @param projectName   project name
     * @param framework     framework
     * @return Map of [instance: plugin instance, configuration: Map of resolved configuration properties], or null
     */
    <T> ConfiguredPlugin<T> configurePlugin(
            String name, Map<String, Object> configuration, String projectName, IFramework framework, Class<T> type
    );

    /**
     * Configure a new plugin using only instance-scope configuration values
     *
     * @param name          provider name
     * @param configuration map of instance configuration values
     * @param service       service
     * @return Map of [instance: plugin instance, configuration: Map of resolved configuration properties], or null
     */
    <T> ConfiguredPlugin<T> configurePlugin(
            String name,
            Map<String, Object> configuration,
            String projectName,
            IFramework framework,
            PluggableProviderService<T> service
    );

    <T> SimplePluginProviderLoader<T> createSimplePluginLoader(
            String projectName, IFramework framework, PluggableProviderService<T> service
    );

    /**
     * Configure a plugin given only instance configuration
     *
     * @param name          name
     * @param configuration instance configuration
     * @param service       service
     * @return plugin , or null if configuration or plugin loading failed
     */


    <T> ConfiguredPlugin<T> configurePlugin(String name, String service, Map<String, Object> configuration);

    /**
     * Configure a plugin given only instance configuration
     *
     * @param name          name
     * @param configuration instance configuration
     * @param type          class
     * @return plugin , or null if configuration or plugin loading failed
     */


    <T> ConfiguredPlugin<T> configurePlugin(String name, Map<String, Object> configuration, Class<T> type);

    /**
     * Configure a plugin given only instance configuration
     *
     * @param name          name
     * @param configuration instance configuration
     * @param service       service
     * @return plugin, or null if configuration or plugin loading failed
     */
    <T> ConfiguredPlugin<T> configurePlugin(
            String name, Map<String, Object> configuration, PluggableProviderService<T> service
    );
}
