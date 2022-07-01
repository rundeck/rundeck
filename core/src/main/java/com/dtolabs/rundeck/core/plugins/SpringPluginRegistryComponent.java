package com.dtolabs.rundeck.core.plugins;

/**
 * Defines a method for PluginRegistryComponent to register static bean mapping for spring context
 */
public interface SpringPluginRegistryComponent {
    /**
     * Register a plugin into map using type and name as key to load it when requested
     *
     * @param type
     * @param name
     * @param beanName
     */
    void registerPlugin(String type, String name, String beanName);
}
