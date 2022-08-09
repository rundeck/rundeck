package com.dtolabs.rundeck.core.plugins;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.IFramework;
import com.dtolabs.rundeck.core.execution.service.ProviderLoaderException;
import com.dtolabs.rundeck.core.plugins.configuration.*;
import com.dtolabs.rundeck.core.utils.IPropertyLookup;
import com.dtolabs.rundeck.plugins.CorePluginProviderServices;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Plugin Registry using a list of {@link PluginRegistryComponent} objects.  Methods which require loading a plugin will
 * iterate over the components to source the plugin, providing the first non-null result from any component.  Any
 * exception thrown by a component will cause the plugin loading to halt and return null.
 */
public class PluginRegistryImpl
        implements PluginRegistry

{
    @Getter @Setter private List<PluginRegistryComponent> components;

    @Override

    public <T> boolean isFrameworkDependentPluginType(Class<T> type) {
        return CorePluginProviderServices.isFrameworkDependentPluginType(type);
    }

    @Override
    public <T> PluggableProviderService<T> getFrameworkDependentPluggableService(
            final Class<T> type,
            final Framework framework
    )
    {
        return CorePluginProviderServices.getPluggableProviderServiceForType(type, framework);
    }

    @Override
    public <T> ConfiguredPlugin<T> configurePluginByName(
            final String name,
            final PluggableProviderService<T> service,
            final Map configuration
    )
    {
        final PropertyResolver resolver = PropertyResolverFactory.createInstanceResolver(configuration);
        return configurePluginByName(name, service, resolver, PropertyScope.InstanceOnly);
    }

    @Override
    public <T> ConfiguredPlugin<T> configurePluginByName(
            final String name,
            final PluggableProviderService<T> service,
            final IFramework framework,
            final String project,
            final Map instanceConfiguration
    )
    {


        final PropertyResolver resolver = PropertyResolverFactory.createFrameworkProjectRuntimeResolver(
                framework,
                project,
                instanceConfiguration,
                service.getName(),
                name
        );
        return configurePluginByName(name, service, resolver, PropertyScope.Instance);
    }

    @Override
    public <T> ConfiguredPlugin<T> configurePluginByName(
            final String name,
            final PluggableProviderService<T> service,
            final IPropertyLookup frameworkLookup,
            final IPropertyLookup projectLookup,
            final Map instanceConfiguration
    )
    {

        final PropertyResolver
                resolver =
                PropertyResolverFactory.createFrameworkProjectRuntimeResolver(
                        frameworkLookup,
                        projectLookup,
                        instanceConfiguration,
                        name,
                        service.getName()
                );
        return configurePluginByName(name, service, resolver, PropertyScope.Instance);
    }

    @Override
    public <T> ConfiguredPlugin<T> configurePluginByName(
            final String name,
            final PluggableProviderService<T> service,
            final PropertyResolver resolver,
            final PropertyScope defaultScope
    )
    {
        DescribedPlugin<T> pluginDesc = loadPluginDescriptorByName(name, service);
        if (null == pluginDesc) {
            return null;
        }
        T plugin = pluginDesc.getInstance();
        Description description = pluginDesc.getDescription();
        Map<String, Object> config = null;
        if (description != null) {
            config = PluginAdapterUtility.configureProperties(resolver, description, plugin, defaultScope);

        }
        return new ConfiguredPlugin<T>(plugin, config);
    }

    @Override
    public <T> ConfiguredPlugin<T> retainConfigurePluginByName(
            final String name,
            final PluggableProviderService<T> service,
            final PropertyResolver resolver,
            final PropertyScope defaultScope
    )
    {
        CloseableDescribedPlugin<T> pluginDesc = retainPluginDescriptorByName(name, service);
        if (null == pluginDesc) {
            return null;
        }
        T plugin = pluginDesc.getInstance();
        Description description = pluginDesc.getDescription();
        Map<String, Object> config = null;
        if (description != null) {
            config = PluginAdapterUtility.configureProperties(resolver, description, plugin, defaultScope);
        }
        return new ConfiguredPlugin<T>(plugin, config, pluginDesc.getCloseable());
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
    )
    {
        if (!components.stream().allMatch(c -> c.isAllowed(name, service))) {
            return null;
        }
        for (PluginRegistryComponent component : components) {
            CloseableDescribedPlugin<T> tDescribedPlugin = null;
            try {
                tDescribedPlugin = component.retainPluginDescriptorByName(name, service);
            } catch (Exception e) {
                return null;
            }
            if (null != tDescribedPlugin) {
                return tDescribedPlugin;
            }
        }
        return null;
    }

    @Override
    public <T> Map<String, Object> getPluginConfigurationByName(
            final String name,
            final PluggableProviderService<T> service,
            final PropertyResolver resolver,
            final PropertyScope defaultScope
    )
    {
        DescribedPlugin<T> pluginDesc = loadPluginDescriptorByName(name, service);
        if (null == pluginDesc) {
            return null;
        }
        Description description = pluginDesc.getDescription();
        Map<String, Object> config = new HashMap<>();
        if (description != null) {
            config = PluginAdapterUtility.mapDescribedProperties(resolver, description, defaultScope);
        }
        return config;
    }

    @Override
    public ValidatedPlugin validatePluginByName(
            final String name,
            final PluggableProviderService service,
            final PropertyResolver resolver,
            final PropertyScope defaultScope
    )
    {
        return validatePluginByName(name, service, resolver, defaultScope, null);
    }

    @Override
    public ValidatedPlugin validatePluginByName(
            final String name,
            final PluggableProviderService service,
            final PropertyResolver resolver,
            final PropertyScope defaultScope,
            final PropertyScope ignoredScope
    )
    {
        DescribedPlugin pluginDesc = loadPluginDescriptorByName(name, service);
        if (null == pluginDesc) {
            return null;
        }
        ValidatedPlugin result = new ValidatedPlugin();
        Description description = pluginDesc.getDescription();
        if (description != null) {
            Validator.Report report = Validator.validate(resolver, description, defaultScope, ignoredScope);
            result.valid = report.isValid();
            result.report = report;
        }
        return result;
    }

    @Override
    public ValidatedPlugin validatePluginByName(
            final String name,
            final PluggableProviderService service,
            final IFramework framework,
            final String project,
            final Map instanceConfiguration
    )
    {
        final PropertyResolver
                resolver =
                PropertyResolverFactory.createFrameworkProjectRuntimeResolver(
                        framework,
                        project,
                        instanceConfiguration,
                        service.getName(),
                        name
                );
        return validatePluginByName(name, service, resolver, PropertyScope.Instance);
    }

    @Override
    public ValidatedPlugin validatePluginByName(
            final String name,
            final PluggableProviderService service,
            final Map instanceConfiguration
    )
    {
        final PropertyResolver resolver = PropertyResolverFactory.createInstanceResolver(instanceConfiguration);
        return validatePluginByName(name, service, resolver, PropertyScope.InstanceOnly);
    }

    @Override
    public ValidatedPlugin validatePluginByName(
            final String name,
            final PluggableProviderService service,
            final Map instanceConfiguration,
            final PropertyScope ignoredScope
    )
    {
        final PropertyResolver resolver = PropertyResolverFactory.createInstanceResolver(instanceConfiguration);
        return validatePluginByName(name, service, resolver, PropertyScope.InstanceOnly, ignoredScope);
    }

    @Override
    public <T> T loadPluginByName(final String name, final PluggableProviderService<T> service) {
        DescribedPlugin<T> tDescribedPlugin = loadPluginDescriptorByName(name, service);
        if (tDescribedPlugin != null) {
            return tDescribedPlugin.getInstance();
        }
        return null;
    }

    @Override
    public <T> CloseableProvider<T> retainPluginByName(final String name, final PluggableProviderService<T> service) {

        CloseableDescribedPlugin<T> tCloseableDescribedPlugin = retainPluginDescriptorByName(name, service);
        if (null != tCloseableDescribedPlugin) {
            return tCloseableDescribedPlugin.getCloseable();
        }
        return null;
    }

    @Override
    public <T> DescribedPlugin<T> loadPluginDescriptorByName(
            final String name,
            final PluggableProviderService<T> service
    )
    {
        if (!components.stream().allMatch(c -> c.isAllowed(name, service))) {
            return null;
        }
        for (PluginRegistryComponent component : components) {
            DescribedPlugin<T> tDescribedPlugin = null;
            try {
                tDescribedPlugin = component.loadPluginDescriptorByName(name, service);
            } catch (Exception e) {
                return null;
            }
            if (null != tDescribedPlugin) {
                return tDescribedPlugin;
            }
        }
        return null;
    }

    @Override
    public <T> Map<String, Object> listPlugins(
            final Class<T> type,
            final PluggableProviderService<T> service
    )
    {
        Map<String, DescribedPlugin<T>> descriptors = listPluginDescriptors(type, service);
        Map<String, Object> map = new HashMap<>();
        for (Map.Entry<String, DescribedPlugin<T>> entry : descriptors.entrySet()) {
            map.put(entry.getKey(), entry.getValue().getInstance());
        }
        return map;
    }

    @Override
    public <T> Map<String, DescribedPlugin<T>> listPluginDescriptors(
            final Class<T> type,
            final PluggableProviderService<T> service
    )
    {
        Map<String, DescribedPlugin<T>> map = new HashMap<>();
        for (PluginRegistryComponent component : components) {
            Map<String, DescribedPlugin<T>> componentDescriptors = component.listPluginDescriptors(type, service);
            //remove disallowed items
            if (componentDescriptors != null) {
                for (String s : componentDescriptors.keySet()) {
                    DescribedPlugin<T> tDescribedPlugin = componentDescriptors.get(s);
                    if (components.stream().allMatch(c -> c.isAllowed(tDescribedPlugin.getName(), service))) {
                        map.put(s, tDescribedPlugin);
                    }
                }
            }
        }
        return map;
    }

    @Override
    public PluginResourceLoader getResourceLoader(final String service, final String provider)
            throws ProviderLoaderException
    {
        for (PluginRegistryComponent component : components) {
            PluginResourceLoader pluginResourceLoader = null;
            try {
                pluginResourceLoader = component.getResourceLoader(service, provider);
            } catch (Exception e) {
                return null;
            }
            if (null != pluginResourceLoader) {
                return pluginResourceLoader;
            }
        }
        return null;
    }

    @Override
    public PluginMetadata getPluginMetadata(final String service, final String provider)
            throws ProviderLoaderException
    {
        for (PluginRegistryComponent component : components) {
            PluginMetadata componentValue = null;
            try {
                componentValue = component.getPluginMetadata(service, provider);
            } catch (Exception e) {
                return null;
            }
            if (null != componentValue) {
                return componentValue;
            }
        }
        return null;
    }

    @Override
    public void registerPlugin(final String type, final String name, final String beanName) {
        for (PluginRegistryComponent component : components) {
            if (component instanceof SpringPluginRegistryComponent) {
                ((SpringPluginRegistryComponent) component).registerPlugin(type, name, beanName);
                return;
            }
        }
    }
}
