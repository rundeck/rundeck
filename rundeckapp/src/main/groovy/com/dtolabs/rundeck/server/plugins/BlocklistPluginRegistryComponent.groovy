package com.dtolabs.rundeck.server.plugins

import com.dtolabs.rundeck.core.execution.service.ProviderLoaderException
import com.dtolabs.rundeck.core.plugins.*
import groovy.transform.CompileStatic
import org.rundeck.security.RundeckPluginBlocklist

@CompileStatic
class BlocklistPluginRegistryComponent implements PluginRegistryComponent {
    RundeckPluginBlocklist rundeckPluginBlocklist


    @Override
    def <T> CloseableDescribedPlugin<T> retainPluginDescriptorByName(
        final String name,
        final PluggableProviderService<T> service
    ) {
        if(rundeckPluginBlocklist.isPluginProviderPresent(service.name, name)){
            throw new Exception("Plugin is blocked")
        }
        return null
    }

    @Override
    def <T> DescribedPlugin<T> loadPluginDescriptorByName(
        final String name,
        final PluggableProviderService<T> service
    ) {
        if(rundeckPluginBlocklist.isPluginProviderPresent(service.name, name)){
            throw new Exception("Plugin is blocked")
        }
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
