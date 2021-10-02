package org.rundeck.app.authorization.domain.project

import com.dtolabs.rundeck.core.authorization.AuthContextProcessor
import groovy.transform.CompileStatic
import org.rundeck.core.auth.AuthConstants
import org.rundeck.core.auth.access.BaseAuthorizingIdResource
import org.rundeck.core.auth.access.ProjectIdentifier
import org.rundeck.core.auth.access.Singleton

import javax.security.auth.Subject

@CompileStatic
class AppAuthorizingProjectAdhoc extends BaseAuthorizingIdResource<Singleton, ProjectIdentifier>
    implements AuthorizingProjectAdhoc {

    final String resourceTypeName = 'Adhoc Command'

    AppAuthorizingProjectAdhoc(
        final AuthContextProcessor rundeckAuthContextProcessor,
        final Subject subject,
        final ProjectIdentifier identifier
    ) {
        super(rundeckAuthContextProcessor, subject, identifier)
    }

    @Override
    protected Map authresMapForResource(final Singleton resource) {
        return AuthConstants.RESOURCE_ADHOC
    }

    @Override
    boolean exists() {
        return true
    }

    @Override
    protected Singleton retrieve() {
        return Singleton.ONLY
    }

    @Override
    String getResourceIdent() {
        return identifier.project
    }

    @Override
    protected String getProject(final Singleton resource) {
        identifier.project
    }

}
