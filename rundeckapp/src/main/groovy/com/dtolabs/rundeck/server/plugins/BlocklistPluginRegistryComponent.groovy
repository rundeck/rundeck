package com.dtolabs.rundeck.server.plugins

import com.dtolabs.rundeck.core.execution.service.ProviderLoaderException
import com.dtolabs.rundeck.core.plugins.*
import groovy.transform.CompileStatic
import org.rundeck.security.RundeckPluginBlocklist

/**
 * Uses PluginBlocklist to determine if a plugin is allowed or not
 */
@CompileStatic
class BlocklistPluginRegistryComponent implements PluginRegistryComponent {
    PluginBlocklist rundeckPluginBlocklist

    @Override
    def <T> boolean isAllowed(final String name, final PluggableProviderService<T> service) {
        return !rundeckPluginBlocklist.isPluginProviderPresent(service.name, name)
    }

    @Override
    def <T> CloseableDescribedPlugin<T> retainPluginDescriptorByName(
        final String name,
        final PluggableProviderService<T> service
    ) {
        return null
    }

    @Override
    def <T> DescribedPlugin<T> loadPluginDescriptorByName(
        final String name,
        final PluggableProviderService<T> service
    ) {
        return null
    }

    @Override
    def <T> Map<String, DescribedPlugin<T>> listPluginDescriptors(
        final Class<T> pluginType,
        final PluggableProviderService<T> service
    ) {
        return [:]
    }

    @Override
    PluginResourceLoader getResourceLoader(final String service, final String provider) throws ProviderLoaderException {
        return null
    }

    @Override
    PluginMetadata getPluginMetadata(final String service, final String provider) throws ProviderLoaderException {
        return null
    }

}
