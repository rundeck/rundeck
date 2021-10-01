package org.rundeck.app.authorization.domain.project

import org.rundeck.core.auth.access.Accessor
import org.rundeck.core.auth.access.AuthorizedIdResource
import org.rundeck.core.auth.access.ProjectIdentifier
import rundeck.Project

interface AuthorizedProject extends AuthorizedIdResource<Project, ProjectIdentifier> {

    Accessor<Project> getDeleteExecution()
}