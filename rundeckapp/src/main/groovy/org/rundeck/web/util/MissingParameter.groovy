package org.rundeck.web.util

import groovy.transform.CompileStatic

@CompileStatic
class MissingParameter extends Exception{
    List<String> parameters

    MissingParameter(final List<String> parameters) {
        super("Required parameters were missing: $parameters")
        this.parameters = parameters
    }
}
