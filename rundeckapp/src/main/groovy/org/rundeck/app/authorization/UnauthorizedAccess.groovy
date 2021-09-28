package org.rundeck.app.authorization

import groovy.transform.CompileStatic

/**
 * Authorization check failed
 */
@CompileStatic
class UnauthorizedAccess extends Exception {
    /**
     * Action
     */
    String action
    /**
     * Resource type
     */
    String type
    /**
     * Resource name
     */
    String name

    UnauthorizedAccess(String action, String type, String name) {
        super("Unauthorized for ${action} access to ${type} ${name}")
        this.action = action
        this.type = type
        this.name = name
    }
}
