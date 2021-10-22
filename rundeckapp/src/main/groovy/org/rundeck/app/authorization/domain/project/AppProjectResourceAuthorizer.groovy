package org.rundeck.app.authorization.domain.project

import groovy.transform.CompileStatic
import org.rundeck.app.authorization.domain.BaseResourceIdAuthorizer
import org.rundeck.core.auth.access.ProjectIdentifier

import javax.security.auth.Subject

@CompileStatic
class AppProjectResourceAuthorizer extends BaseResourceIdAuthorizer implements ProjectResourceAuthorizer {
    @Override
    AuthorizingProject getAuthorizingResource(final Subject subject, final ProjectIdentifier identifier) {
        return new AppAuthorizingProject(rundeckAuthContextProcessor, subject, identifier)
    }
}