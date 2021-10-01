package org.rundeck.app.authorization.domain.project

import groovy.transform.CompileStatic
import org.rundeck.app.authorization.domain.BaseAuthorizedIdAccess
import org.rundeck.core.auth.access.ProjectIdentifier

import javax.security.auth.Subject

@CompileStatic
class AppProjectAuthorizedAccess extends BaseAuthorizedIdAccess implements ProjectAuthorizedAccess {
    @Override
    AuthorizedProject accessResource(final Subject subject, final ProjectIdentifier identifier) {
        return new AppAuthorizedProject(rundeckAuthContextProcessor, subject, identifier)
    }
}