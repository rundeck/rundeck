package org.rundeck.app.authorization.domain.project

import groovy.transform.CompileStatic
import org.rundeck.core.auth.access.AuthorizedIdAccess
import org.rundeck.core.auth.access.ProjectIdentifier
import rundeck.Project

@CompileStatic
interface ProjectAuthorizedAccess extends AuthorizedIdAccess<Project, AuthorizedProject, ProjectIdentifier> {
}
