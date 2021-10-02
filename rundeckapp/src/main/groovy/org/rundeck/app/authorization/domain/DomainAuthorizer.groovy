package org.rundeck.app.authorization.domain

import org.rundeck.app.authorization.domain.execution.AuthorizingExecution
import org.rundeck.app.authorization.domain.execution.ExecIdentifier
import org.rundeck.app.authorization.domain.project.AuthorizingProject
import org.rundeck.app.authorization.domain.project.AuthorizingProjectAdhoc
import org.rundeck.app.authorization.domain.system.AuthorizingSystem
import org.rundeck.core.auth.access.ProjectIdentifier

import javax.security.auth.Subject

interface DomainAuthorizer {

    AuthorizingExecution execution(Subject subject, ExecIdentifier identifier)

    AuthorizingProject project(Subject subject, ProjectIdentifier identifier)

    AuthorizingProjectAdhoc adhoc(Subject subject, ProjectIdentifier identifier)

    AuthorizingSystem system(Subject subject)
}