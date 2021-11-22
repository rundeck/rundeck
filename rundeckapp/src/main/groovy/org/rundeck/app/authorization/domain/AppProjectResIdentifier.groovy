package org.rundeck.app.authorization.domain

import groovy.transform.CompileStatic
import org.rundeck.core.auth.access.ProjectResIdentifier

@CompileStatic
class AppProjectResIdentifier implements ProjectResIdentifier {
    final String project
    final String id

    AppProjectResIdentifier(final String project, final String id) {
        this.project = project
        this.id = id
    }
}
