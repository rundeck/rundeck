package org.rundeck.app.authorization.domain.projectAcl

import groovy.transform.CompileStatic
import org.rundeck.app.authorization.domain.BaseResourceIdAuthorizingProvider
import org.rundeck.core.auth.access.NamedAuthProvider
import org.rundeck.core.auth.access.ResIdResolver
import org.rundeck.core.auth.access.Singleton
import org.rundeck.core.auth.app.RundeckAccess
import org.rundeck.core.auth.app.type.AuthorizingProjectAcl
import org.springframework.beans.factory.annotation.Autowired

import javax.security.auth.Subject

@CompileStatic
class AppProjectAclAuthorizingProvider extends BaseResourceIdAuthorizingProvider<Singleton, AuthorizingProjectAcl, String> {
    @Autowired
    NamedAuthProvider namedAuthProvider;

    AppAuthorizingProjectAcl getAuthorizingResource(final Subject subject, String project) {
        return new AppAuthorizingProjectAcl(
            rundeckAuthContextProcessor,
            subject,
            namedAuthProvider,
            project
        )
    }

    @Override
    AuthorizingProjectAcl getAuthorizingResource(final Subject subject, final ResIdResolver resolver) {
        return getAuthorizingResource(
            subject,
            resolver.idForType(RundeckAccess.Project.TYPE)
        )
    }
}
