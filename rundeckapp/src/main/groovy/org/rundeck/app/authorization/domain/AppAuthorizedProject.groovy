package org.rundeck.app.authorization.domain

import grails.compiler.GrailsCompileStatic
import groovy.transform.CompileStatic
import org.rundeck.app.authorization.AccessActions
import org.rundeck.app.authorization.AccessLevels
import org.rundeck.app.authorization.AppAuthContextProcessor
import org.rundeck.app.authorization.BaseAuthorizedResource
import org.rundeck.app.authorization.NotFound
import org.rundeck.app.authorization.ProjectIdentifier
import org.rundeck.app.authorization.UnauthorizedAccess
import org.rundeck.core.auth.AuthConstants
import rundeck.Project

import javax.security.auth.Subject

@GrailsCompileStatic
class AppAuthorizedProject extends BaseAuthorizedResource<Project, ProjectIdentifier> implements AuthorizedProject {
    public static final AccessActions APP_DELETE_EXECUTION =
        AccessLevels.any(
            [AuthConstants.ACTION_DELETE_EXECUTION],
            AccessLevels.APP_ADMIN
        )
    final String resourceTypeName = 'Project'


    AppAuthorizedProject(
        final AppAuthContextProcessor rundeckAuthContextProcessor,
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
    protected Project retrieve(final ProjectIdentifier identifier) {
        //TODO
        return Project.findByName(identifier.project)
    }

    @Override
    String getPrimary(final ProjectIdentifier identifier) {
        return identifier.project
    }

    @Override
    protected String getProject(final Project resource) {
        resource.name
    }

    @Override
    Project requireDeleteExecution() throws UnauthorizedAccess, NotFound {
        requireActions(APP_DELETE_EXECUTION)
    }

    @Override
    boolean hasDeleteExecution() throws NotFound {
        return canPerform(APP_DELETE_EXECUTION)
    }
}
