package com.dtolabs.rundeck.server.plugins

import com.dtolabs.rundeck.core.plugins.configuration.Description

/**
 * DescribedPlugin holds a plugin instance, name, description and source file if available
 * @author greg
 * @since 2014-02-19
 */
class DescribedPlugin<T> {
    DescribedPlugin(T instance, Description description, String name) {
        this.instance = instance
        this.description = description
        this.name = name
    }

    DescribedPlugin(T instance, Description description, String name, File file) {
        this.name = name
        this.instance = instance
        this.description = description
        this.file = file
    }

    String name
    T instance
    Description description
    File file
}
