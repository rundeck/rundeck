package org.rundeck.app.authorization.domain.project

import groovy.transform.CompileStatic
import org.rundeck.core.auth.access.ProjectIdentifier

@CompileStatic
class AppProjectIdentifier implements ProjectIdentifier {
    final String project

    AppProjectIdentifier(String project) {
        this.project = project
    }
}
