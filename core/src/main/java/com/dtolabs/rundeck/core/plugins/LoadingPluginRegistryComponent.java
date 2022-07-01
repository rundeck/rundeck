package com.dtolabs.rundeck.core.plugins;

import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException;
import com.dtolabs.rundeck.core.execution.service.MissingProviderException;
import com.dtolabs.rundeck.core.execution.service.ProviderLoaderException;
import com.dtolabs.rundeck.core.plugins.configuration.DescribableServiceUtil;
import com.dtolabs.rundeck.core.plugins.configuration.Description;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry component that uses the ServiceProviderLoader mechanism
 */
@Slf4j
@RequiredArgsConstructor
public class LoadingPluginRegistryComponent
        implements PluginRegistryComponent

{
    private final ServiceProviderLoader rundeckServerServiceProviderLoader;

    @Override
    public <T> CloseableDescribedPlugin<T> retainPluginDescriptorByName(
            final String name,
            final PluggableProviderService<T> service
    )
    {
        //try loading via ServiceProviderLoader
        //attempt to load directly
        CloseableProvider<T> instance = null;
        try {
            instance = service.closeableProviderOfType(name);
        } catch (ExecutionServiceException ignored) {
            //not already loaded, attempt to load...
        }
        if (null == instance) {
            try {
                instance = rundeckServerServiceProviderLoader.loadCloseableProvider(service, name);
            } catch (MissingProviderException exception) {
                log.error(String.format("Plugin %s for service: %s was not found", name, service.getName()));
                log.debug(String.format("Plugin %s for service: %s was not found", name, service.getName()), exception);
            } catch (ProviderLoaderException exception) {
                log.error(String.format(
                        "Failure loading Rundeck plugin: %s for service: : %s",
                        name,
                        service.getName()
                ), exception);
            }
        }
        if (null != instance) {
            return new CloseableDescribedPlugin<T>(instance, loadPluginDescription(service, name), name);
        }
        return null;
    }

    private Description loadPluginDescription(PluggableProviderService<?> service, String name) {
        return DescribableServiceUtil.loadDescriptionForType(service, name, true);
    }

    @Override
    public <T> DescribedPlugin<T> loadPluginDescriptorByName(
            final String name,
            final PluggableProviderService<T> service
    )
    {
        //attempt to load directly
        T instance = null;
        try {
            instance = service.providerOfType(name);
        } catch (ExecutionServiceException ignored) {
            //not already loaded, attempt to load...
        }
        if (null == instance) {
            try {
                instance = rundeckServerServiceProviderLoader.loadProvider(service, name);
            } catch (MissingProviderException exception) {
                log.error(String.format("Plugin %s for service: %s was not found", name, service.getName()));
                log.debug(String.format("Plugin %s for service: %s was not found", name, service.getName()), exception);
            } catch (ProviderLoaderException exception) {
                log.error(String.format(
                        "Failure loading Rundeck plugin: %s for service: : %s",
                        name,
                        service.getName()
                ), exception);
            }
        }
        if (null != instance) {
            return new DescribedPlugin<T>(instance, loadPluginDescription(service, name), name);
        }
        return null;
    }

    @Override
    public <T> Map<String, DescribedPlugin<T>> listPluginDescriptors(
            final Class<T> pluginType,
            final PluggableProviderService<T> service
    )
    {
        Map<String, DescribedPlugin<T>> map = new HashMap<>();
        for (ProviderIdent ident : service.listProviders()) {
            T instance = null;
            try {
                instance = service.providerOfType(ident.getProviderName());
                if (null == instance) {
                    instance = rundeckServerServiceProviderLoader.loadProvider(service, ident.getProviderName());
                }
            } catch (ExecutionServiceException ignored) {
            }
            if (instance == null || !pluginType.isAssignableFrom(instance.getClass())) {
                continue;
            }

            map.putIfAbsent(ident.getProviderName(), new DescribedPlugin<T>(instance, null, ident.getProviderName()));
        }
        for (Description d : service.listDescriptions()) {

            if (!map.containsKey(d.getName())) {
                map.put(d.getName(), new DescribedPlugin<T>(null, d, d.getName()));
            }
            map.get(d.getName()).setDescription(d);
        }
        return map;
    }

    @Override
    public PluginResourceLoader getResourceLoader(final String service, final String provider)
            throws ProviderLoaderException
    {
        return rundeckServerServiceProviderLoader.getResourceLoader(service, provider);
    }

    @Override
    public PluginMetadata getPluginMetadata(final String service, final String provider)
            throws ProviderLoaderException
    {
        return rundeckServerServiceProviderLoader.getPluginMetadata(service, provider);
    }
}
