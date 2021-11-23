package org.rundeck.app.authorization.domain.appType

import groovy.transform.CompileStatic
import org.rundeck.app.authorization.domain.BaseResourceIdAuthorizingProvider
import org.rundeck.core.auth.access.NamedAuthProvider
import org.rundeck.core.auth.access.ResIdResolver
import org.rundeck.core.auth.access.Singleton
import org.rundeck.core.auth.app.RundeckAccess
import org.rundeck.core.auth.app.type.AuthorizingAppType
import org.springframework.beans.factory.annotation.Autowired

import javax.security.auth.Subject

/**
 * Creates AuthorizingAppType
 */
@CompileStatic
class AppResourceTypeAuthorizingProvider extends BaseResourceIdAuthorizingProvider<Singleton, AuthorizingAppType, String> {
    @Autowired
    NamedAuthProvider namedAuthProvider

    AuthorizingAppType getAuthorizingResource(final Subject subject, String type) {
        return new AppAuthorizingAppType(
            rundeckAuthContextProcessor,
            subject,
            namedAuthProvider,
            type
        )
    }

    @Override
    AuthorizingAppType getAuthorizingResource(final Subject subject, ResIdResolver resolver) {
        return getAuthorizingResource(
            subject,
            resolver.idForType(RundeckAccess.ApplicationType.TYPE)
        )
    }
}
