package org.rundeck.app.authorization.domain.execution

import com.dtolabs.rundeck.core.authorization.AuthContextProcessor
import com.dtolabs.rundeck.core.authorization.AuthResource
import com.dtolabs.rundeck.core.authorization.AuthorizationUtil
import grails.compiler.GrailsCompileStatic
import org.rundeck.core.auth.access.ProjectResIdentifier
import org.rundeck.core.auth.access.BaseAuthorizingIdResource
import org.rundeck.core.auth.AuthConstants
import org.rundeck.core.auth.access.NamedAuthProvider
import org.rundeck.core.auth.access.NotFound
import org.rundeck.core.auth.access.UnauthorizedAccess
import org.rundeck.core.auth.app.RundeckAccess
import rundeck.Execution

import javax.security.auth.Subject

@GrailsCompileStatic
class AppAuthorizingExecution extends BaseAuthorizingIdResource<Execution, ProjectResIdentifier>
    implements AuthorizingExecution {
    final String resourceTypeName = 'Execution'


    AppAuthorizingExecution(
        final AuthContextProcessor rundeckAuthContextProcessor,
        final Subject subject,
        final NamedAuthProvider namedAuthActions,
        final ProjectResIdentifier identifier
    ) {
        super(rundeckAuthContextProcessor, subject, namedAuthActions, identifier)
    }

    @Override
    protected AuthResource getAuthResource(final Execution resource) {
        return AuthorizationUtil.projectAuthResource(
            resource.scheduledExecution ?
            rundeckAuthContextProcessor.authResourceForJob(
                resource.scheduledExecution.jobName,
                resource.scheduledExecution.groupPath,
                resource.scheduledExecution.uuid
            ) :
            AuthConstants.RESOURCE_ADHOC
        )
    }

    @Override
    boolean exists() {
        if(identifier.project){
            return Execution.countByIdAndProject(identifier.id.toLong(), identifier.project) == 1
        }
        return Execution.countById(identifier.id.toLong()) == 1
    }

    private Execution found

    @Override
    protected Optional<Execution> retrieve() {
        if (null != found) {
            return Optional.of(found)
        }
        if (identifier.project) {
            found = Execution.findByIdAndProject(identifier.id.toLong(), identifier.project)
        } else {
            try {
                found = Execution.get(identifier.id.toLong())
            } catch(NumberFormatException ignored) {
                found = Execution.findByUuid(identifier.id)
            }
        }
        Optional.ofNullable(found)
    }


    @Override
    String getResourceIdent() {
        return identifier.id
    }

    @Override
    protected String getProject() {
        if (identifier.project) {
            return identifier.project
        } else {
            retrieve().get().project
        }
    }

    public Execution getReadOrView() throws UnauthorizedAccess, NotFound {
        return access(RundeckAccess.Execution.APP_READ_OR_VIEW)
    }

    public Execution getKill() throws UnauthorizedAccess, NotFound {
        return access(RundeckAccess.Execution.APP_KILL)
    }

    public Execution getKillAs() throws UnauthorizedAccess, NotFound {
        return access(RundeckAccess.Execution.APP_KILLAS)
    }

}
