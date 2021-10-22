package org.rundeck.app.authorization.domain.project

import com.dtolabs.rundeck.core.authorization.AuthContextProcessor
import grails.compiler.GrailsCompileStatic
import org.rundeck.core.auth.access.AuthActions
import org.rundeck.core.auth.access.AccessLevels
import org.rundeck.core.auth.access.Accessor
import org.rundeck.core.auth.access.BaseAuthorizingIdResource
import org.rundeck.core.auth.access.ProjectIdentifier
import org.rundeck.core.auth.AuthConstants
import rundeck.Project

import javax.security.auth.Subject

/**
 * Authorized access for a project
 */
@GrailsCompileStatic
class AppAuthorizingProject extends BaseAuthorizingIdResource<Project, ProjectIdentifier>
    implements AuthorizingProject {
    public static final AuthActions APP_DELETE_EXECUTION =
        AccessLevels.or(
            [AuthConstants.ACTION_DELETE_EXECUTION],
            AccessLevels.APP_ADMIN
        )
    final String resourceTypeName = 'Project'


    AppAuthorizingProject(
        final AuthContextProcessor rundeckAuthContextProcessor,
        final Subject subject,
        final ProjectIdentifier identifier
    ) {
        super(rundeckAuthContextProcessor, subject, identifier)
    }

    @Override
    protected Map authresMapForResource(final Project resource) {
        return rundeckAuthContextProcessor.authResourceForProject(resource.name)
    }

    @Override
    boolean exists() {
        return Project.countByName(identifier.project) == 1
    }

    @Override
    protected Project retrieve() {
        //TODO
        return Project.findByName(identifier.project)
    }

    @Override
    String getResourceIdent() {
        return identifier.project
    }

    @Override
    protected String getProject(final Project resource) {
        resource.name
    }

    public Accessor<Project> getDeleteExecution() {
        return access(APP_DELETE_EXECUTION);
    }

}
