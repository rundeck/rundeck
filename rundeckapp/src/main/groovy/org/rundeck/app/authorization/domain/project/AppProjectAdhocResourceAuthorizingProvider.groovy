package org.rundeck.app.authorization.domain.project

import groovy.transform.CompileStatic
import org.rundeck.app.authorization.domain.BaseResourceIdAuthorizingProvider
import org.rundeck.core.auth.access.NamedAuthProvider
import org.rundeck.core.auth.access.ProjectIdentifier
import org.rundeck.core.auth.access.ResIdResolver
import org.rundeck.core.auth.access.Singleton
import org.rundeck.core.auth.app.RundeckAccess
import org.rundeck.core.auth.app.type.AuthorizingProjectAdhoc
import org.springframework.beans.factory.annotation.Autowired

import javax.security.auth.Subject

@CompileStatic
class AppProjectAdhocResourceAuthorizingProvider
    extends BaseResourceIdAuthorizingProvider<Singleton, AuthorizingProjectAdhoc, ProjectIdentifier> {
    @Autowired
    NamedAuthProvider namedAuthProvider;

    AuthorizingProjectAdhoc getAuthorizingResource(final Subject subject, String project) {
        return new AppAuthorizingProjectAdhoc(
            rundeckAuthContextProcessor,
            subject,
            namedAuthProvider,
            project
        )
    }

    @Override
    AuthorizingProjectAdhoc getAuthorizingResource(final Subject subject, final ResIdResolver resolver) {
        return getAuthorizingResource(
            subject,
            resolver.idForType(RundeckAccess.Project.TYPE)
        )
    }
}