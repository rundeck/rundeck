package org.rundeck.app.authorization.domain.project

import groovy.transform.CompileStatic
import org.rundeck.core.auth.access.ResourceIdAuthorizer
import org.rundeck.core.auth.access.ProjectIdentifier
import rundeck.Project

@CompileStatic
interface ProjectResourceAuthorizer extends ResourceIdAuthorizer<Project, AuthorizingProject, ProjectIdentifier> {
}
