package org.rundeck.app.authorization.domain.job

import com.dtolabs.rundeck.core.authorization.AuthContextProcessor
import com.dtolabs.rundeck.core.authorization.AuthResource
import com.dtolabs.rundeck.core.authorization.AuthorizationUtil
import grails.compiler.GrailsCompileStatic
import org.rundeck.core.auth.access.ProjectResIdentifier
import org.rundeck.core.auth.access.BaseAuthorizingIdResource
import org.rundeck.core.auth.access.NamedAuthProvider
import rundeck.ScheduledExecution

import javax.security.auth.Subject

@GrailsCompileStatic
class AppAuthorizingJob extends BaseAuthorizingIdResource<ScheduledExecution, ProjectResIdentifier>
    implements AuthorizingJob {
    final String resourceTypeName = 'Job'

    AppAuthorizingJob(
        final AuthContextProcessor rundeckAuthContextProcessor,
        final Subject subject,
        final NamedAuthProvider namedAuthActions,
        final ProjectResIdentifier identifier
    ) {
        super(rundeckAuthContextProcessor, subject, namedAuthActions, identifier)
    }

    @Override
    protected AuthResource getAuthResource(final ScheduledExecution resource) {
        return AuthorizationUtil.projectAuthResource(
            rundeckAuthContextProcessor.authResourceForJob(
                resource.jobName,
                resource.groupPath,
                resource.uuid
            )
        )
    }

    @Override
    protected Optional<ScheduledExecution> retrieve() {
        ScheduledExecution found
        if (identifier.project) {
            found = ScheduledExecution.findByUuidAndProject(identifier.id, identifier.project)
        } else {
            found = ScheduledExecution.findByUuid(identifier.id)
        }
        Optional.ofNullable(found)
    }

    @Override
    boolean exists() {
        if (identifier.project) {
            return ScheduledExecution.countByUuidAndProject(identifier.id, identifier.project) == 1
        }
        return ScheduledExecution.countByUuid(identifier.id) == 1
    }

    @Override
    protected String getProject() {
        if (identifier.project) {
            return identifier.project
        } else {
            retrieve().get().project
        }
    }

    @Override
    protected String getResourceIdent() {
        return identifier.id
    }
}
