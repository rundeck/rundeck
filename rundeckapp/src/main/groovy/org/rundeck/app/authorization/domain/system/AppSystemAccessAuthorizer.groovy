package org.rundeck.app.authorization.domain.system

import groovy.transform.CompileStatic
import org.rundeck.app.authorization.domain.BaseAccessAuthorizer

import javax.security.auth.Subject

@CompileStatic
class AppSystemAccessAuthorizer extends BaseAccessAuthorizer<AuthorizingSystem>
    implements SystemAccessAuthorizer {
    @Override
    AuthorizingSystem getAuthorizingResource(final Subject subject) {
        return new AppAuthorizingSystem(rundeckAuthContextProcessor, subject)
    }
}
