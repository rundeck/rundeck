package com.dtolabs.rundeck.core.plugins;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.ProviderService;

import java.util.Map;

/**
 * Created by greg on 5/5/16.
 */
public class ServiceFactory {

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

    public static <T> PluggableProviderService<T> pluginService(
            final String serviceName,
            final Framework framework,
            final Class<T> providerClass
    )
    {
        FrameworkPluggableProviderService<T> frameworkPluggableProviderService = new
                FrameworkPluggableProviderService<T>(
                serviceName,
                framework,
                providerClass
        )
        {
            @Override
            public boolean isScriptPluggable() {
                return false;
            }
        };
        return frameworkPluggableProviderService;
    }
}
