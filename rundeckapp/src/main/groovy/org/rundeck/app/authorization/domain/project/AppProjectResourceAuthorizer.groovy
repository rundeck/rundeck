package org.rundeck.app.authorization.domain.project

import groovy.transform.CompileStatic
import org.rundeck.app.authorization.domain.BaseResourceIdAuthorizer
import org.rundeck.core.auth.access.MissingParameter
import org.rundeck.core.auth.access.NamedAuthProvider
import org.rundeck.core.auth.access.ProjectIdentifier
import org.rundeck.core.auth.access.ResIdResolver
import org.rundeck.core.auth.app.RundeckAccess
import org.springframework.beans.factory.annotation.Autowired
import rundeck.Project

import javax.security.auth.Subject

@CompileStatic
class AppProjectResourceAuthorizer extends BaseResourceIdAuthorizer<Project, AuthorizingProject, ProjectIdentifier> {
    @Autowired
    NamedAuthProvider namedAuthProvider
    @Autowired
    ProjectManager projectManagerService

    @Override
    AuthorizingProject getAuthorizingResource(final Subject subject, final ResIdResolver resolver)
        throws MissingParameter {
        return new AppAuthorizingProject(
            rundeckAuthContextProcessor,
            subject,
            namedAuthProvider,
            resolver.idForType(RundeckAccess.Project.TYPE),
            projectManagerService
        )
    }
}