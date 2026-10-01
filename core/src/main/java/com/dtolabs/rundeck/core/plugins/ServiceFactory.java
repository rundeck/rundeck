package com.dtolabs.rundeck.core.plugins;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.IFramework;

import java.util.Map;

/**
 * Created by greg on 5/5/16.
 */
public class ServiceFactory {

    /**
     *
     * @param framework
     * @param serviceName
     * @param classes
     * @param <T>
     * @return
     * @deprecated
     */
    @Deprecated
    public static <T> BaseProviderRegistryService<T> builtinService(
            final Framework framework,
            final String serviceName,
            final Map<String, Class<? extends T>> classes
    )
    {
        return new BaseProviderRegistryService<T>(framework, classes) {
            @Override
            public String getName() {
                return serviceName;
            }
        };
    }

    public static <T> IFrameworkProviderRegistryService<T> builtinService(
            final IFramework framework,
            final String serviceName,
            final Map<String, Class<? extends T>> classes
    )
    {
        return new IFrameworkProviderRegistryService<T>(framework, classes) {
            @Override
            public String getName() {
                return serviceName;
            }
        };
    }

    public static <T> ProviderRegistryService<T> builtinService(
            final String serviceName,
            final Map<String, Class<? extends T>> classes
    )
    {
        return new AbstractProviderRegistryService<T>(classes) {
            @Override
            public String getName() {
                return serviceName;
            }
        };
    }

    /**
     * @param serviceName
     * @param framework
     * @param providerClass
     * @param <T>
     * @deprecated use {@link #pluginService(String, Class, ServiceProviderLoader)}
     */
    @Deprecated
    public static <T> PluggableProviderService<T> pluginService(
            final String serviceName,
            final Framework framework,
            final Class<T> providerClass
    )
    {
        return new
                FrameworkPluggableProviderService<T>(
                serviceName,
                framework,
                providerClass
        );
    }

    /**
     * Creates a pluggable service using a provider loader
     *
     * @param serviceName
     * @param providerClass
     * @param serviceProviderLoader
     * @param <T>
     */
    public static <T> PluggableProviderService<T> pluginService(
            final String serviceName,
            final Class<T> providerClass,
            final ServiceProviderLoader serviceProviderLoader
    )
    {
        return new
                BasePluggableProviderServiceImpl<T>(
                serviceName,
                providerClass,
                serviceProviderLoader
        );
    }
}
