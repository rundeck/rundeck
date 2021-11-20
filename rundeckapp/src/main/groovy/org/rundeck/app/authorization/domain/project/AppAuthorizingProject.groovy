package org.rundeck.app.authorization.domain.project

import com.dtolabs.rundeck.core.authorization.AuthContextProcessor
import com.dtolabs.rundeck.core.authorization.AuthResource
import com.dtolabs.rundeck.core.authorization.AuthorizationUtil
import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.common.ProjectManager
import grails.compiler.GrailsCompileStatic
import org.rundeck.app.auth.types.AuthorizingProject
import org.rundeck.core.auth.access.*
import org.rundeck.core.auth.app.RundeckAccess
import rundeck.Project

import javax.security.auth.Subject
/**
 * Authorized access for a project
 */
@GrailsCompileStatic
class AppAuthorizingProject extends BaseAuthorizingIdResource<IRundeckProject, String>
    implements AuthorizingProject {
    final ProjectManager projectManager

    final String resourceTypeName = 'Project'

    AppAuthorizingProject(
        final AuthContextProcessor rundeckAuthContextProcessor,
        final Subject subject,
        final NamedAuthProvider namedAuthActions,
        final String identifier,
        final ProjectManager projectManager
    ) {
        super(rundeckAuthContextProcessor, subject, namedAuthActions, identifier)
        this.projectManager = projectManager
    }

    @Override
    protected AuthResource getAuthResource(final IRundeckProject resource) {
        return AuthorizationUtil.systemAuthResource(
            rundeckAuthContextProcessor.authResourceForProject(resource.name)
        )
    }

    @Override
    boolean exists() {
        return Project.countByName(identifier) == 1
    }

    @Override
    protected Project retrieve() {
        //TODO
        return Project.findByName(identifier)
    }

    @Override
    String getResourceIdent() {
        return identifier
    }

    @Override
    protected String getProject() {
        identifier
    }

    public Project getConfigure() throws UnauthorizedAccess, NotFound {
        return access(RundeckAccess.Project.APP_CONFIGURE);
    }

    public Project getDeleteExecution() throws UnauthorizedAccess, NotFound {
        return access(RundeckAccess.Project.APP_DELETE_EXECUTION);
    }

    public Project getExport() throws UnauthorizedAccess, NotFound {
        return access(RundeckAccess.Project.APP_EXPORT);
    }

    public Project getImport() throws UnauthorizedAccess, NotFound {
        return access(RundeckAccess.Project.APP_IMPORT);
    }

    public Project getPromote() throws UnauthorizedAccess, NotFound {
        return access(RundeckAccess.Project.APP_PROMOTE);
    }

}
