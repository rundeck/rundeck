package org.rundeck.app.authorization.domain.project

import com.dtolabs.rundeck.core.authorization.AuthContextProcessor
import grails.compiler.GrailsCompileStatic
import org.rundeck.core.auth.access.AuthActions
import org.rundeck.core.auth.access.AccessLevels
import org.rundeck.core.auth.access.Accessor
import org.rundeck.core.auth.access.BaseAuthorizingIdResource
import org.rundeck.core.auth.access.NotFound
import org.rundeck.core.auth.access.ProjectIdentifier
import org.rundeck.core.auth.AuthConstants
import org.rundeck.core.auth.access.UnauthorizedAccess
import rundeck.Project

import javax.security.auth.Subject

/**
 * Authorized access for a project
 */
@GrailsCompileStatic
class AppAuthorizingProject extends BaseAuthorizingIdResource<Project, ProjectIdentifier>
    implements AuthorizingProject {

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

    public Project getConfigure() throws UnauthorizedAccess, NotFound {
        return access(APP_CONFIGURE);
    }

    public Project getDeleteExecution() throws UnauthorizedAccess, NotFound {
        return access(APP_DELETE_EXECUTION);
    }

    public Project getExport() throws UnauthorizedAccess, NotFound {
        return access(APP_EXPORT);
    }

    public Project getImport() throws UnauthorizedAccess, NotFound {
        return access(APP_IMPORT);
    }

    public Project getPromote() throws UnauthorizedAccess, NotFound {
        return access(APP_PROMOTE);
    }

}
