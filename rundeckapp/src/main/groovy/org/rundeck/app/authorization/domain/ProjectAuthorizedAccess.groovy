package org.rundeck.app.authorization.domain

import groovy.transform.CompileStatic
import org.rundeck.app.authorization.AuthorizedAccess
import org.rundeck.app.authorization.ProjectIdentifier
import rundeck.Execution
import rundeck.Project

@CompileStatic
interface ProjectAuthorizedAccess extends AuthorizedAccess<Project, AuthorizedProject, ProjectIdentifier> {
}
