package com.dtolabs.rundeck.server.plugins

/**
 * ConfiguredPlugin holds a plugin instance and configuration map
 * @author greg
 * @since 2014-02-19
 */
class ConfiguredPlugin<T> {
    ConfiguredPlugin(T instance, Map<String, Object> configuration) {
        this.instance = instance
        this.configuration = configuration
    }

    T instance
    Map<String,Object> configuration
}
