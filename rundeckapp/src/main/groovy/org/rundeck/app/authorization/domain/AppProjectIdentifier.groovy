package org.rundeck.app.authorization.domain

import groovy.transform.CompileStatic
import org.rundeck.app.authorization.ProjectIdentifier

@CompileStatic
class AppProjectIdentifier implements ProjectIdentifier {
    final String project

    AppProjectIdentifier(String project) {
        this.project = project
    }
}
