package org.rundeck.app.authorization.domain.appType

import com.dtolabs.rundeck.core.authorization.AuthContextProcessor
import com.dtolabs.rundeck.core.authorization.AuthResource
import com.dtolabs.rundeck.core.authorization.AuthorizationUtil
import groovy.transform.CompileStatic
import org.rundeck.core.auth.access.BaseAuthorizingIdResource
import org.rundeck.core.auth.access.NamedAuthProvider
import org.rundeck.core.auth.access.NotFound
import org.rundeck.core.auth.access.Singleton
import org.rundeck.core.auth.app.type.AuthorizingAppType

import javax.security.auth.Subject

/**
 * Authorizing app type implementation
 */
@CompileStatic
class AppAuthorizingAppType extends BaseAuthorizingIdResource<Singleton, String> implements AuthorizingAppType {
    final String resourceTypeName = 'Resource'
    final String project = null
    final boolean authContextWithProject = false

    @Override
    protected AuthResource getAuthResource(final Singleton resource) {
        return getAuthResource()
    }

    @Override
    protected String getResourceIdent() {
        return identifier
    }

    @Override
    protected Optional<Singleton> retrieve() {
        return Optional.of(Singleton.ONLY)
    }

    @Override
    protected AuthResource getAuthResource() throws NotFound {
        return AuthorizationUtil.authResource(
            AuthResource.Context.System,
            AuthorizationUtil.resourceType(identifier)
        )
    }

    AppAuthorizingAppType(
        final AuthContextProcessor rundeckAuthContextProcessor,
        final Subject subject,
        final NamedAuthProvider namedAuthActions,
        final String identifier
    ) {
        super(rundeckAuthContextProcessor, subject, namedAuthActions, identifier)
    }

    @Override
    boolean exists() {
        return true
    }
}
