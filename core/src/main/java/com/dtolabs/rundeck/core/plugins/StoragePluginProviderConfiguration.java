package com.dtolabs.rundeck.core.plugins;

interface StoragePluginProviderConfiguration extends PluginProviderConfiguration {
    String getPath();
    Boolean getPathPrefix();
}
