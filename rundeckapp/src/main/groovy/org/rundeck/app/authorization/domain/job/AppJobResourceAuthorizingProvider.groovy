package org.rundeck.app.authorization.domain.job

import groovy.transform.CompileStatic
import org.rundeck.app.authorization.domain.BaseResourceIdAuthorizingProvider
import org.rundeck.app.authorization.domain.AppProjectResIdentifier
import org.rundeck.app.data.model.v1.job.JobData
import org.rundeck.core.auth.access.ProjectResIdentifier
import org.rundeck.core.auth.access.NamedAuthProvider
import org.rundeck.core.auth.access.ResIdResolver
import org.rundeck.core.auth.app.RundeckAccess
import org.springframework.beans.factory.annotation.Autowired
import rundeck.services.RdJobService

import javax.security.auth.Subject

@CompileStatic
class AppJobResourceAuthorizingProvider
    extends BaseResourceIdAuthorizingProvider<JobData, AuthorizingJob, ProjectResIdentifier> {

    @Autowired
    NamedAuthProvider namedAuthProvider;
    @Autowired
    RdJobService rdJobService

    AuthorizingJob getAuthorizingResource(final Subject subject, String project, String id) {
        return new AppAuthorizingJob(
            rundeckAuthContextProcessor,
            subject,
            namedAuthProvider,
            new AppProjectResIdentifier(project, id),
            rdJobService
        )
    }

    @Override
    AuthorizingJob getAuthorizingResource(final Subject subject, final ResIdResolver resolver) {
        return getAuthorizingResource(
            subject,
            resolver.idForTypeOptional(RundeckAccess.Project.TYPE).orElse(null),
            resolver.idForType(RundeckAccess.Job.TYPE)
        )
    }
}

