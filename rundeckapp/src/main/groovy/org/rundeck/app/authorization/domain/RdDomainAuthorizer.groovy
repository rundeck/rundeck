package org.rundeck.app.authorization.domain

import groovy.transform.CompileStatic
import org.rundeck.app.authorization.domain.execution.AppExecIdentifier
import org.rundeck.app.authorization.domain.execution.AppExecutionResourceAuthorizer
import org.rundeck.app.authorization.domain.execution.AuthorizingExecution
import org.rundeck.app.authorization.domain.execution.ExecIdentifier
import org.rundeck.app.authorization.domain.project.AppProjectAdhocResourceAuthorizer
import org.rundeck.app.authorization.domain.project.AppProjectResourceAuthorizer
import org.rundeck.app.authorization.domain.project.AppProjectIdentifier
import org.rundeck.app.authorization.domain.project.AuthorizingProject
import org.rundeck.app.authorization.domain.project.AuthorizingProjectAdhoc
import org.rundeck.app.authorization.domain.system.AppSystemResourceAuthorizer
import org.rundeck.app.authorization.domain.system.AuthorizingSystem
import org.rundeck.core.auth.access.ProjectIdentifier
import org.springframework.beans.factory.annotation.Autowired

import javax.security.auth.Subject

/**
 * Consolidated access to authorized resources
 */
@CompileStatic
class RdDomainAuthorizer implements DomainAuthorizer{
    @Autowired
    AppExecutionResourceAuthorizer rundeckExecutionAuthorizer
    @Autowired
    AppProjectResourceAuthorizer rundeckProjectAuthorizer
    @Autowired
    AppSystemResourceAuthorizer rundeckSystemAuthorizer
    @Autowired
    AppProjectAdhocResourceAuthorizer rundeckProjectAdhocAuthorizer


    static ExecIdentifier executionId(String id, String project) {
        new AppExecIdentifier(project, id)
    }

    static ProjectIdentifier projectId(String project) {
        new AppProjectIdentifier(project)
    }

    @Override
    AuthorizingExecution execution(Subject subject, ExecIdentifier identifier) {
        rundeckExecutionAuthorizer.getAuthorizingResource(subject, identifier)
    }

    @Override
    AuthorizingProject project(Subject subject, ProjectIdentifier identifier) {
        rundeckProjectAuthorizer.getAuthorizingResource(subject, identifier)
    }

    @Override
    AuthorizingProjectAdhoc adhoc(Subject subject, ProjectIdentifier identifier) {
        rundeckProjectAdhocAuthorizer.getAuthorizingResource(subject, identifier)
    }

    @Override
    AuthorizingSystem system(Subject subject) {
        rundeckSystemAuthorizer.getAuthorizingResource(subject)
    }
}
