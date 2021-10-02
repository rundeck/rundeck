package org.rundeck.app.authorization.domain.project

import org.rundeck.core.auth.access.Accessor
import org.rundeck.core.auth.access.AuthorizingIdResource
import org.rundeck.core.auth.access.ProjectIdentifier
import rundeck.Project

interface AuthorizingProject extends AuthorizingIdResource<Project, ProjectIdentifier> {

    Accessor<Project> getDeleteExecution()
}