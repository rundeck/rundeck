package org.rundeck.app.authorization.domain

import org.rundeck.app.authorization.domain.execution.AuthorizingExecution
import org.rundeck.app.auth.types.AuthorizingProject
import org.rundeck.core.auth.app.type.AuthorizingProjectAdhoc
import org.rundeck.core.auth.access.ResIdResolver
import org.rundeck.core.auth.app.DomainAuthorizer

import javax.security.auth.Subject

interface AppDomainAuthorizer extends DomainAuthorizer {

    AuthorizingExecution execution(Subject subject, ResIdResolver resolver)

    AuthorizingProject project(Subject subject, ResIdResolver resolver)
    AuthorizingProject project(Subject subject, String project)

}