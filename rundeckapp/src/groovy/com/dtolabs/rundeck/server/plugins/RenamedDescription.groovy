package com.dtolabs.rundeck.server.plugins

import com.dtolabs.rundeck.core.plugins.configuration.Description

/**
 * RenamedDescription overrides the name property of a {@link Description}
 * @author greg
 * @since 2014-02-19
 */
class RenamedDescription{
    @Delegate
    Description delegate
    String name
}
