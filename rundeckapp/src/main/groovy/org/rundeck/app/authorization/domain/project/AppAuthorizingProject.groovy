package org.rundeck.app.authorization.domain.project

import com.dtolabs.rundeck.core.authorization.AuthContextProcessor
import com.dtolabs.rundeck.core.authorization.AuthResource
import com.dtolabs.rundeck.core.authorization.AuthorizationUtil
import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.common.ProjectManager
import grails.compiler.GrailsCompileStatic
import org.rundeck.app.auth.types.AuthorizingProject
import org.rundeck.core.auth.access.*
import rundeck.services.ProjectService

import javax.security.auth.Subject
/**
 * Authorized access for a project
 */
@GrailsCompileStatic
class AppAuthorizingProject extends BaseAuthorizingIdResource<IRundeckProject, String>
    implements AuthorizingProject {
    final ProjectManager projectManager
    final ProjectService projectService

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
        return projectManager.existsFrameworkProject(identifier)
    }

    @Override
    protected Optional<IRundeckProject> retrieve() {
        if(projectManager.existsFrameworkProject(identifier)){
            return Optional.ofNullable(projectManager.getFrameworkProject(identifier))
        }
        return Optional.empty()
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
