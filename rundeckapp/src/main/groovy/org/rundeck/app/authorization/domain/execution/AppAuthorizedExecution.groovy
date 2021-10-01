package org.rundeck.app.authorization.domain.execution

import com.dtolabs.rundeck.core.authorization.AuthContextProcessor
import grails.compiler.GrailsCompileStatic
import org.rundeck.core.auth.access.AccessActions
import org.rundeck.core.auth.access.AccessLevels
import org.rundeck.core.auth.access.Accessor
import org.rundeck.core.auth.access.BaseAuthorizedIdResource
import org.rundeck.core.auth.AuthConstants
import rundeck.Execution

import javax.security.auth.Subject

@GrailsCompileStatic
class AppAuthorizedExecution extends BaseAuthorizedIdResource<Execution, ExecIdentifier>
    implements AuthorizedExecution {
    final String resourceTypeName = 'Execution'

    final static AccessActions APP_KILL = AccessLevels.any([AuthConstants.ACTION_KILL], AccessLevels.APP_ADMIN)
    final static AccessActions APP_KILLAS = AccessLevels.any([AuthConstants.ACTION_KILLAS], AccessLevels.APP_ADMIN)


    AppAuthorizedExecution(
        final AuthContextProcessor rundeckAuthContextProcessor,
        final Subject subject,
        final ExecIdentifier identifier
    ) {
        super(rundeckAuthContextProcessor, subject, identifier)
    }

    @Override
    protected Map authresMapForResource(final Execution resource) {
        return resource.scheduledExecution ?
               rundeckAuthContextProcessor.authResourceForJob(
                   resource.scheduledExecution.jobName,
                   resource.scheduledExecution.groupPath,
                   resource.scheduledExecution.uuid
               ) :
               AuthConstants.RESOURCE_ADHOC
    }

    @Override
    boolean exists() {
        return Execution.countById(identifier.id.toLong()) == 1
    }

    @Override
    protected Execution retrieve() {
        return Execution.get(identifier.id)
    }


    @Override
    String getPrimaryIdComponent() {
        return identifier.id
    }

    @Override
    protected String getProject(final Execution resource) {
        resource.project
    }

    public Accessor<Execution> getReadOrView() {
        return access(AccessLevels.APP_READ_OR_VIEW);
    }

    public Accessor<Execution> getKill() {
        return access(APP_KILL);
    }
    public Accessor<Execution> getKillAs() {
        return access(APP_KILLAS);
    }

}
