package org.rundeck.app.authorization.domain

import groovy.transform.CompileStatic
import org.rundeck.app.authorization.domain.execution.AppExecIdentifier
import org.rundeck.app.authorization.domain.execution.AppExecutionAuthorizedAccess
import org.rundeck.app.authorization.domain.execution.AuthorizedExecution
import org.rundeck.app.authorization.domain.execution.ExecIdentifier
import org.rundeck.app.authorization.domain.project.AppProjectAdhocAuthorizedAccess
import org.rundeck.app.authorization.domain.project.AppProjectAuthorizedAccess
import org.rundeck.app.authorization.domain.project.AppProjectIdentifier
import org.rundeck.app.authorization.domain.project.AuthorizedProject
import org.rundeck.app.authorization.domain.project.AuthorizedProjectAdhoc
import org.rundeck.app.authorization.domain.system.AppSystemAuthorizedAccess
import org.rundeck.app.authorization.domain.system.AuthorizedSystem
import org.rundeck.core.auth.access.ProjectIdentifier
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
    @Autowired
    AppSystemAuthorizedAccess rundeckSystemAuthorizedAccess
    @Autowired
    AppProjectAdhocAuthorizedAccess rundeckProjectAdhocAuthorizedAccess


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

    AuthorizedProjectAdhoc adhoc(Subject subject, ProjectIdentifier identifier) {
        rundeckProjectAdhocAuthorizedAccess.accessResource(subject, identifier)
    }

    AuthorizedSystem system(Subject subject) {
        rundeckSystemAuthorizedAccess.accessResource(subject)
    }
}
