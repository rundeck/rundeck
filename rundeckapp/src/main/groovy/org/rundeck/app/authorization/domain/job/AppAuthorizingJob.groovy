package org.rundeck.app.authorization.domain.job

import com.dtolabs.rundeck.core.authorization.AuthContextProcessor
import com.dtolabs.rundeck.core.authorization.AuthResource
import com.dtolabs.rundeck.core.authorization.AuthorizationUtil
import grails.compiler.GrailsCompileStatic
import org.rundeck.app.data.model.v1.job.JobData
import org.rundeck.core.auth.access.ProjectResIdentifier
import org.rundeck.core.auth.access.BaseAuthorizingIdResource
import org.rundeck.core.auth.access.NamedAuthProvider
import rundeck.services.RdJobService

import javax.security.auth.Subject

@GrailsCompileStatic
class AppAuthorizingJob extends BaseAuthorizingIdResource<JobData, ProjectResIdentifier>
    implements AuthorizingJob {
    final String resourceTypeName = 'Job'
    final RdJobService rdJobService

    AppAuthorizingJob(
        final AuthContextProcessor rundeckAuthContextProcessor,
        final Subject subject,
        final NamedAuthProvider namedAuthActions,
        final ProjectResIdentifier identifier,
        final RdJobService rdJobService
    ) {
        super(rundeckAuthContextProcessor, subject, namedAuthActions, identifier)
        this.rdJobService = rdJobService
    }

    @Override
    protected AuthResource getAuthResource(final JobData resource) {
        return AuthorizationUtil.projectAuthResource(
            rundeckAuthContextProcessor.authResourceForJob(
                resource.jobName,
                resource.groupPath,
                resource.uuid
            )
        )
    }

    @Override
    protected Optional<JobData> retrieve() {
        Optional.ofNullable(rdJobService.getJobByUuid(identifier.id))
    }

    @Override
    boolean exists() {
        return rdJobService.existsByUuid(identifier.id)
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
