package com.dtolabs.rundeck.core.plugins;

public interface PluginServiceCapabilities extends PluginDescriptionService {
    PluginRegistry getPluginRegistry();
    <T> PluggableProviderService<T> createPluggableService(Class<T> type);
}
