package org.rundeck.app.authorization.domain.system

import groovy.transform.CompileStatic
import org.rundeck.app.authorization.domain.BaseAuthorizingProvider
import org.rundeck.core.auth.access.NamedAuthProvider
import org.rundeck.core.auth.access.ResIdResolver
import org.rundeck.core.auth.app.type.AuthorizingSystem
import org.springframework.beans.factory.annotation.Autowired

import javax.security.auth.Subject

@CompileStatic
class AppSystemAuthorizingProvider extends BaseAuthorizingProvider<AuthorizingSystem> {

    @Autowired
    NamedAuthProvider namedAuthProvider

    @Override
    AuthorizingSystem getAuthorizingResource(final Subject subject, ResIdResolver resolver) {
        return new AppAuthorizingSystem(rundeckAuthContextProcessor, subject, namedAuthProvider)
    }
}
