package org.rundeck.app.authorization.domain.projectType

import groovy.transform.CompileStatic
import org.rundeck.core.auth.app.type.ProjectTypeIdentifier

/**
 * Identifier for project type
 */
@CompileStatic
class AppProjectTypeIdentifier implements ProjectTypeIdentifier {

    final String project
    final String type

    AppProjectTypeIdentifier(final String project, final String type) {
        this.project = project
        this.type = type
    }
}
