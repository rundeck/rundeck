package org.rundeck.app.authorization.domain.projectType

import groovy.transform.CompileStatic
import org.rundeck.app.authorization.domain.BaseResourceIdAuthorizingProvider
import org.rundeck.core.auth.access.NamedAuthProvider
import org.rundeck.core.auth.access.ResIdResolver
import org.rundeck.core.auth.access.Singleton
import org.rundeck.core.auth.app.RundeckAccess
import org.rundeck.core.auth.app.type.AuthorizingProjectType
import org.rundeck.core.auth.app.type.ProjectTypeIdentifier
import org.springframework.beans.factory.annotation.Autowired

import javax.security.auth.Subject

/**
 * Creates AuthorizingProjectType
 */
@CompileStatic
class AppProjectTypeAuthorizingProvider
    extends BaseResourceIdAuthorizingProvider<Singleton, AuthorizingProjectType, ProjectTypeIdentifier> {

    @Autowired
    NamedAuthProvider namedAuthProvider

    AuthorizingProjectType getAuthorizingResource(final Subject subject, String project, String type) {
        return new AppAuthorizingProjectType(
            rundeckAuthContextProcessor,
            subject,
            namedAuthProvider,
            new AppProjectTypeIdentifier(
                project,
                type
            )
        )
    }

    @Override
    AuthorizingProjectType getAuthorizingResource(final Subject subject, ResIdResolver resolver) {
        return getAuthorizingResource(
            subject,
            resolver.idForType(RundeckAccess.Project.TYPE),
            resolver.idForType(RundeckAccess.ApplicationType.TYPE)
        )
    }
}