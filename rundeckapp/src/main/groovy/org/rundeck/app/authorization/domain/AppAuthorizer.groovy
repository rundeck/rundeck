package org.rundeck.app.authorization.domain

import org.rundeck.app.auth.CoreTypedRequestAuthorizer
import org.rundeck.app.authorization.domain.execution.AuthorizingExecution
import org.rundeck.app.authorization.domain.job.AuthorizingJob
import org.rundeck.core.auth.access.ResIdResolver

import javax.security.auth.Subject

/**
 * Authorizing access to rundeckapp resource types
 */
interface AppAuthorizer extends CoreTypedRequestAuthorizer {

    AuthorizingExecution execution(Subject subject, ResIdResolver resolver)
    AuthorizingExecution execution(final Subject subject, final String project, final String id)

    AuthorizingJob job(Subject subject, ResIdResolver resolver)
    AuthorizingJob job(Subject subject,String project, String id)
}