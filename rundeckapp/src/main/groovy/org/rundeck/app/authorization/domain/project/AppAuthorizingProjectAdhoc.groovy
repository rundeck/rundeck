package org.rundeck.app.authorization.domain.project

import com.dtolabs.rundeck.core.authorization.AuthContextProcessor
import com.dtolabs.rundeck.core.authorization.AuthResource
import com.dtolabs.rundeck.core.authorization.AuthorizationUtil
import groovy.transform.CompileStatic
import org.rundeck.core.auth.AuthConstants
import org.rundeck.core.auth.access.BaseAuthorizingIdResource
import org.rundeck.core.auth.access.NamedAuthProvider
import org.rundeck.core.auth.access.Singleton
import org.rundeck.core.auth.app.type.AuthorizingProjectAdhoc

import javax.security.auth.Subject

@CompileStatic
class AppAuthorizingProjectAdhoc extends BaseAuthorizingIdResource<Singleton, String>
    implements AuthorizingProjectAdhoc {

    final String resourceTypeName = 'Adhoc Command'

    AppAuthorizingProjectAdhoc(
        final AuthContextProcessor rundeckAuthContextProcessor,
        final Subject subject,
        final NamedAuthProvider namedAuthActions,
        final String identifier
    ) {
        super(rundeckAuthContextProcessor, subject, namedAuthActions, identifier)
    }

    @Override
    protected AuthResource getAuthResource(final Singleton resource) {
        return AuthorizationUtil.projectAuthResource(AuthConstants.RESOURCE_ADHOC)
    }

    @Override
    boolean exists() {
        return true
    }

    @Override
    protected Optional<Singleton> retrieve() {
        return Optional.of(Singleton.ONLY)
    }

    @Override
    String getResourceIdent() {
        return identifier
    }

    @Override
    protected String getProject() {
        identifier
    }

}
