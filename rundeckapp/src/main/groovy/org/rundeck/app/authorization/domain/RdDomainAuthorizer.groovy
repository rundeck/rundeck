package org.rundeck.app.authorization.domain

import groovy.transform.CompileStatic
import org.rundeck.app.authorization.domain.execution.AppExecIdentifier
import org.rundeck.app.authorization.domain.execution.AppExecutionResourceAuthorizer
import org.rundeck.app.authorization.domain.execution.AuthorizingExecution
import org.rundeck.app.authorization.domain.execution.ExecIdentifier
import org.rundeck.app.authorization.domain.project.AppProjectAdhocResourceAuthorizer
import org.rundeck.app.authorization.domain.project.AppProjectResourceAuthorizer

import org.rundeck.app.authorization.domain.project.AuthorizingProject
import org.rundeck.app.authorization.domain.project.AuthorizingProjectAdhoc
import org.rundeck.app.authorization.domain.system.AppSystemAccessAuthorizer
import org.rundeck.core.auth.app.type.AuthorizingSystem
import org.rundeck.core.auth.access.AuthorizingAccess
import org.rundeck.core.auth.access.MissingParameter
import org.rundeck.core.auth.access.NotFound
import org.rundeck.core.auth.access.ProjectIdentifier
import org.rundeck.core.auth.access.UnauthorizedAccess
import org.rundeck.core.auth.app.RundeckAccess
import org.rundeck.core.auth.access.ResIdResolver
import org.rundeck.core.auth.app.TypedNamedAuthRequest
import org.springframework.beans.factory.annotation.Autowired

import javax.security.auth.Subject

/**
 * Consolidated access to authorized resources
 */
@CompileStatic
class RdDomainAuthorizer implements AppDomainAuthorizer {
    @Autowired
    AppExecutionResourceAuthorizer rundeckExecutionAuthorizer
    @Autowired
    AppProjectResourceAuthorizer rundeckProjectAuthorizer
    @Autowired
    AppSystemAccessAuthorizer rundeckSystemAuthorizer
    @Autowired
    AppProjectAdhocResourceAuthorizer rundeckProjectAdhocAuthorizer


    @Override
    boolean isAuthorized(final Subject subject, ResIdResolver resolver, final TypedNamedAuthRequest request)
        throws MissingParameter {
        def access = getAuthorizingAccess(subject, resolver, request.getType())
        return access.isAuthorized(request)
    }

    @Override
    void authorize(final Subject subject, ResIdResolver resolver, final TypedNamedAuthRequest request)
        throws UnauthorizedAccess, NotFound, MissingParameter {
        def access = getAuthorizingAccess(subject, resolver, request.getType())
        access.authorizeNamed(request)
    }

    AuthorizingAccess getAuthorizingAccess(Subject subject, ResIdResolver resolver, String type)
        throws MissingParameter {
        if (type == RundeckAccess.System.TYPE) {
            return system(subject)
        } else if (type == RundeckAccess.Project.TYPE) {
            return project(subject, resolver)
        } else if (type == RundeckAccess.Adhoc.TYPE) {
            return adhoc(subject, resolver)
        } else if (type == RundeckAccess.Execution.TYPE) {
            return execution(subject, resolver)
        }
        throw new IllegalArgumentException("unknown type for authorizing access: " + type)
    }

    @Override
    AuthorizingExecution execution(Subject subject, ResIdResolver resolver) {
        rundeckExecutionAuthorizer.getAuthorizingResource(subject, resolver)
    }

    @Override
    AuthorizingProject project(Subject subject, ResIdResolver resolver) {
        rundeckProjectAuthorizer.getAuthorizingResource(subject, resolver)
    }

    @Override
    AuthorizingProjectAdhoc adhoc(Subject subject, ResIdResolver resolver) {
        rundeckProjectAdhocAuthorizer.getAuthorizingResource(subject, resolver)
    }

    @Override
    AuthorizingSystem system(Subject subject) {
        rundeckSystemAuthorizer.getAuthorizingResource(subject, null)
    }
}
