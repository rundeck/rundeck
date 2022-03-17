package org.rundeck.app.authorization.domain.job

import groovy.transform.CompileStatic
import org.rundeck.core.auth.access.ProjectResIdentifier
import org.rundeck.core.auth.access.AuthorizingIdResource
import rundeck.ScheduledExecution

@CompileStatic
interface AuthorizingJob extends AuthorizingIdResource<ScheduledExecution, ProjectResIdentifier> {
}
