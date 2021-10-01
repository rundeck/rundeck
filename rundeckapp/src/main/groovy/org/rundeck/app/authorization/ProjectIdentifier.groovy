package org.rundeck.app.authorization

import groovy.transform.CompileStatic

@CompileStatic
interface ProjectIdentifier {
    String getProject()
}