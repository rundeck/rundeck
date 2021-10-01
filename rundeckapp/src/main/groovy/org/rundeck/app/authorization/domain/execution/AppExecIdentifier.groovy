package org.rundeck.app.authorization.domain.execution

import groovy.transform.CompileStatic

@CompileStatic
class AppExecIdentifier implements ExecIdentifier {
    final String project
    final String id

    AppExecIdentifier(final String project, final String id) {
        this.project = project
        this.id = id
    }
}
