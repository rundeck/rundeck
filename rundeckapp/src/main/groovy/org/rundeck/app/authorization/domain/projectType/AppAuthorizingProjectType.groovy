package org.rundeck.app.authorization.domain.projectType

import com.dtolabs.rundeck.core.authorization.AuthContextProcessor
import com.dtolabs.rundeck.core.authorization.AuthResource
import com.dtolabs.rundeck.core.authorization.AuthorizationUtil
import groovy.transform.CompileStatic
import org.rundeck.core.auth.access.BaseAuthorizingIdResource
import org.rundeck.core.auth.access.NamedAuthProvider
import org.rundeck.core.auth.access.NotFound
import org.rundeck.core.auth.access.Singleton
import org.rundeck.core.auth.app.type.AuthorizingProjectType
import org.rundeck.core.auth.app.type.ProjectTypeIdentifier

import javax.security.auth.Subject

/**
 * AuthorizingProjectType implementation
 */
@CompileStatic
class AppAuthorizingProjectType extends BaseAuthorizingIdResource<Singleton, ProjectTypeIdentifier>
    implements AuthorizingProjectType {
    final String resourceTypeName = 'Project Resource'
    final boolean authContextWithProject = true

    @Override
    protected AuthResource getAuthResource(final Singleton resource) {
        return getAuthResource()
    }

    @Override
    protected String getResourceIdent() {
        return identifier.type
    }

    @Override
    protected String getProject() {
        return identifier.project
    }

    @Override
    protected Optional<Singleton> retrieve() {
        Optional.of Singleton.ONLY
    }

    @Override
    protected AuthResource getAuthResource() throws NotFound {
        return AuthorizationUtil.authResource(
            AuthResource.Context.Project,
            AuthorizationUtil.resourceType(identifier.type)
        )
    }

    AppAuthorizingProjectType(
        final AuthContextProcessor rundeckAuthContextProcessor,
        final Subject subject,
        final NamedAuthProvider namedAuthActions,
        final ProjectTypeIdentifier identifier
    ) {
        super(rundeckAuthContextProcessor, subject, namedAuthActions, identifier)
    }

    @Override
    boolean exists() {
        return true
    }
}
