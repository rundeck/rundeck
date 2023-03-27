package com.dtolabs.rundeck.core.plugins;

public interface PluginDescriptionService {
    DescribedPlugin getPluginDescriptor(String name, PluggableProviderService service);
    DescribedPlugin getPluginDescriptor(String name, Class type);
    DescribedPlugin getPluginDescriptor(String name, String service);

}
