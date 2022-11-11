package org.rundeck.app.authorization.domain.job

import groovy.transform.CompileStatic
import org.rundeck.app.data.model.v1.job.JobData
import org.rundeck.core.auth.access.ProjectResIdentifier
import org.rundeck.core.auth.access.AuthorizingIdResource

@CompileStatic
interface AuthorizingJob extends AuthorizingIdResource<JobData, ProjectResIdentifier> {
}
