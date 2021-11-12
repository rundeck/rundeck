package org.rundeck.app.authorization.domain.system

import groovy.transform.CompileStatic
import org.rundeck.app.authorization.domain.BaseAccessAuthorizer
import org.rundeck.core.auth.access.AccessAuthorizer
import org.rundeck.core.auth.access.NamedAuthProvider
import org.rundeck.core.auth.access.ResIdResolver
import org.rundeck.core.auth.app.type.AuthorizingSystem
import org.springframework.beans.factory.annotation.Autowired

import javax.security.auth.Subject

@CompileStatic
class AppSystemAccessAuthorizer extends BaseAccessAuthorizer<AuthorizingSystem> {

    @Autowired
    NamedAuthProvider namedAuthProvider

    @Override
    AuthorizingSystem getAuthorizingResource(final Subject subject, ResIdResolver resolver) {
        return new AppAuthorizingSystem(rundeckAuthContextProcessor, subject, namedAuthProvider)
    }
}
