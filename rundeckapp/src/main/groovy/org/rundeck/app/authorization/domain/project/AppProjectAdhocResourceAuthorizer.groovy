package org.rundeck.app.authorization.domain.project

import groovy.transform.CompileStatic
import org.rundeck.app.authorization.domain.BaseResourceIdAuthorizer
import org.rundeck.core.auth.access.ProjectIdentifier

import javax.security.auth.Subject

@CompileStatic
class AppProjectAdhocResourceAuthorizer extends BaseResourceIdAuthorizer implements ProjectAdhocResourceAuthorizer {
    @Override
    AuthorizingProjectAdhoc accessResource(final Subject subject, final ProjectIdentifier identifier) {
        return new AppAuthorizingProjectAdhoc(rundeckAuthContextProcessor, subject, identifier)
    }
}