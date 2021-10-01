package org.rundeck.app.authorization.domain

import grails.compiler.GrailsCompileStatic
import groovy.transform.CompileStatic
import org.rundeck.app.authorization.AccessActions
import org.rundeck.app.authorization.AccessLevels
import org.rundeck.app.authorization.AppAuthContextProcessor
import org.rundeck.app.authorization.BaseAuthorizedResource
import org.rundeck.app.authorization.NotFound
import org.rundeck.app.authorization.UnauthorizedAccess
import org.rundeck.core.auth.AuthConstants
import rundeck.Execution

import javax.security.auth.Subject

@GrailsCompileStatic
class AppAuthorizedExecution extends BaseAuthorizedResource<Execution, ExecIdentifier> implements AuthorizedExecution {
    final String resourceTypeName = 'Execution'

    final static AccessActions APP_KILL = AccessLevels.any([AuthConstants.ACTION_KILL], AccessLevels.APP_ADMIN)


    AppAuthorizedExecution(
        final AppAuthContextProcessor rundeckAuthContextProcessor,
        final Subject subject,
        final ExecIdentifier identifier
    ) {
        super(rundeckAuthContextProcessor, subject, identifier)
    }

    @Override
    protected Map authresMapForResource(final Execution resource) {
        return resource.scheduledExecution ?
               rundeckAuthContextProcessor.authResourceForJob(resource.scheduledExecution) :
               AuthConstants.RESOURCE_ADHOC
    }

    @Override
    boolean exists() {
        return Execution.countById(identifier.id.toLong()) == 1
    }

    @Override
    protected Execution retrieve(final ExecIdentifier execIdentifier) {
        return Execution.get(execIdentifier.id)
    }


    @Override
    String getPrimary(final ExecIdentifier execIdentifier) {
        return execIdentifier.id
    }

    @Override
    protected String getProject(final Execution resource) {
        resource.project
    }

    @Override
    Execution requireReadOrView() throws UnauthorizedAccess, NotFound {
        return requireActions(AccessLevels.APP_READ_OR_VIEW)
    }

    @Override
    boolean canReadOrView() throws NotFound {
        return canPerform(AccessLevels.APP_READ_OR_VIEW)
    }

    @Override
    Execution requireKill() throws UnauthorizedAccess, NotFound {
        requireActions(APP_KILL)
    }

    @Override
    boolean canKill() throws NotFound {
        canPerform(APP_KILL)
    }
}
