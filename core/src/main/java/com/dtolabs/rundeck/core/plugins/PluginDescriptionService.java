package com.dtolabs.rundeck.core.plugins;

import com.dtolabs.rundeck.core.plugins.configuration.Description;
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope;

import java.util.Map;

public interface PluginDescriptionService {
    DescribedPlugin getPluginDescriptor(String name, PluggableProviderService service);
    DescribedPlugin getPluginDescriptor(String name, Class type);
    DescribedPlugin getPluginDescriptor(String name, String service);

}
