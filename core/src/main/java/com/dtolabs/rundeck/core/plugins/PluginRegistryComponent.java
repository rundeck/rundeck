package com.dtolabs.rundeck.core.plugins;

import com.dtolabs.rundeck.core.execution.service.ProviderLoaderException;

import java.util.Map;

/**
 * Contributes to loading plugins for the {@link PluginRegistry}
 */
public interface PluginRegistryComponent {

    /**
     * Check if plugin is allowed
     *
     * @param name    name of bean or provider
     * @param service provider service
     * @return true to allow this plugin, false to prevent it
     */
    public default <T> boolean isAllowed(
            String name,
            PluggableProviderService<T> service
    )
    {
        return true;
    }

    /**
     * Load a closeable plugin instance with the given bean or provider name, should be used when the plugin instance
     * will be retained for use over a period of time
     *
     * @param name    name of bean or provider
     * @param service provider service
     * @return CloseableDescribedPlugin , or null if it cannot be loaded
     */
    public <T> CloseableDescribedPlugin<T> retainPluginDescriptorByName(
            String name,
            PluggableProviderService<T> service
    );

    /**
     * Load a plugin instance with the given bean or provider name
     *
     * @param name    name of bean or provider
     * @param service provider service
     * @return map containing [instance:(plugin instance), description: (map or Description),
     */
    public <T> DescribedPlugin<T> loadPluginDescriptorByName(String name, PluggableProviderService<T> service);

    /**
     * List all plugin type definitions that are either ServiceProvider plugins of the given service name, or are groovy
     * plugins of the given type
     *
     * @param pluginType
     */
    public <T> Map<String, DescribedPlugin<T>> listPluginDescriptors(
            Class<T> pluginType,
            PluggableProviderService<T> service
    );

    /**
     * Return plugin resource loader
     *
     * @param service
     * @param provider
     * @throws ProviderLoaderException
     */
    public PluginResourceLoader getResourceLoader(String service, String provider) throws ProviderLoaderException;

    /**
     * Return plugin file metadata
     *
     * @param service
     * @param provider
     * @throws ProviderLoaderException
     */
    public PluginMetadata getPluginMetadata(String service, String provider) throws ProviderLoaderException;

}
