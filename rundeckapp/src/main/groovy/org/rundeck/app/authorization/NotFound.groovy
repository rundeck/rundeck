package org.rundeck.app.authorization

import groovy.transform.CompileStatic

@CompileStatic
class NotFound extends Exception {
    /**
     * Resource type
     */
    String type
    /**
     * Resource name
     */
    String name

    NotFound(String type, String name) {
        super("Not found: ${type} ${name}")
        this.type = type
        this.name = name
    }
}
