package org.rundeck.app.authorization.domain.project

import groovy.transform.CompileStatic
import org.rundeck.app.authorization.domain.BaseResourceIdAuthorizer
import org.rundeck.core.auth.access.NamedAuthProvider
import org.rundeck.core.auth.access.ProjectIdentifier
import org.rundeck.core.auth.access.ResIdResolver
import org.rundeck.core.auth.access.Singleton
import org.rundeck.core.auth.app.RundeckAccess
import org.springframework.beans.factory.annotation.Autowired

import javax.security.auth.Subject

@CompileStatic
class AppProjectAdhocResourceAuthorizer extends BaseResourceIdAuthorizer<Singleton, AuthorizingProjectAdhoc, ProjectIdentifier> {
    @Autowired
    NamedAuthProvider namedAuthProvider;

    @Override
    AuthorizingProjectAdhoc getAuthorizingResource(final Subject subject, final ResIdResolver resolver) {
        return new AppAuthorizingProjectAdhoc(
            rundeckAuthContextProcessor,
            subject,
            namedAuthProvider,
            resolver.idForType(RundeckAccess.Project.NAME)
        )
    }
}