package org.rundeck.app.authorization.domain

import org.rundeck.app.authorization.domain.execution.AuthorizingExecution
import org.rundeck.app.authorization.domain.execution.ExecIdentifier
import org.rundeck.app.authorization.domain.project.AuthorizingProject
import org.rundeck.app.authorization.domain.project.AuthorizingProjectAdhoc
import org.rundeck.core.auth.access.ResIdResolver
import org.rundeck.core.auth.app.type.AuthorizingSystem
import org.rundeck.core.auth.access.ProjectIdentifier
import org.rundeck.core.auth.app.DomainAuthorizer

import javax.security.auth.Subject

interface AppDomainAuthorizer extends DomainAuthorizer {

    AuthorizingExecution execution(Subject subject, ResIdResolver resolver)

    AuthorizingProject project(Subject subject, ResIdResolver resolver)

    AuthorizingProjectAdhoc adhoc(Subject subject, ResIdResolver resolver)
}