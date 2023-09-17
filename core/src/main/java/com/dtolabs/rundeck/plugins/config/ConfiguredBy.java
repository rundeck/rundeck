package com.dtolabs.rundeck.plugins.config;

public interface ConfiguredBy<T extends PluginGroup> {
    void setPluginGroup(T pluginGroup);
}
