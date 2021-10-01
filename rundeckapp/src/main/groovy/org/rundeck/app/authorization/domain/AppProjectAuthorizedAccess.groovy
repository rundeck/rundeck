package org.rundeck.app.authorization.domain

import groovy.transform.CompileStatic
import org.rundeck.app.authorization.ProjectIdentifier

import javax.security.auth.Subject

@CompileStatic
class AppProjectAuthorizedAccess extends BaseAuthorizedAccess implements ProjectAuthorizedAccess {
    @Override
    AuthorizedProject accessResource(final Subject subject, final ProjectIdentifier identifier) {
        return new AppAuthorizedProject(rundeckAuthContextProcessor, subject, identifier)
    }
}