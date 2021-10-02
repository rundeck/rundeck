package org.rundeck.app.authorization.domain.system

import groovy.transform.CompileStatic
import org.rundeck.app.authorization.domain.BaseResourceAuthorizer
import org.rundeck.core.auth.access.Singleton

import javax.security.auth.Subject

@CompileStatic
class AppSystemResourceAuthorizer extends BaseResourceAuthorizer<Singleton, AuthorizingSystem> implements SystemResourceAuthorizer {
    @Override
    AuthorizingSystem getAuthorizingResource(final Subject subject) {
        return new AppAuthorizingSystem(rundeckAuthContextProcessor, subject)
    }
}
