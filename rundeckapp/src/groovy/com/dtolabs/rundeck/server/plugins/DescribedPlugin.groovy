package com.dtolabs.rundeck.server.plugins

import com.dtolabs.rundeck.core.plugins.configuration.Description

/**
 * DescribedPlugin holds a plugin instance, name, description and source file if available
 * @author greg
 * @since 2014-02-19
 */
class DescribedPlugin<T> {
    String name
    T instance
    Description description
    File file
}
