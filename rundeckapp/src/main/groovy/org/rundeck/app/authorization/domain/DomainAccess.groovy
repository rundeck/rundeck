package org.rundeck.app.authorization.domain

import groovy.transform.CompileStatic
import org.rundeck.app.authorization.ProjectIdentifier
import org.springframework.beans.factory.annotation.Autowired

import javax.security.auth.Subject

/**
 * Consolidated access to authorized resources
 */
@CompileStatic
class DomainAccess {
    @Autowired
    AppExecutionAuthorizedAccess rundeckExecutionAuthorizedAccess
    @Autowired
    AppProjectAuthorizedAccess rundeckProjectAuthorizedAccess


    static ExecIdentifier executionId(String id, String project) {
        new AppExecIdentifier(project, id)
    }

    static ProjectIdentifier projectId(String project) {
        new AppProjectIdentifier(project)
    }

    AuthorizedExecution execution(Subject subject, ExecIdentifier identifier) {
        rundeckExecutionAuthorizedAccess.accessResource(subject, identifier)
    }

    AuthorizedProject project(Subject subject, ProjectIdentifier identifier) {
        rundeckProjectAuthorizedAccess.accessResource(subject, identifier)
    }
}
