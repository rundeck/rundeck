package org.rundeck.app.authorization.domain.project

import groovy.transform.CompileStatic
import org.rundeck.core.auth.access.AuthorizedIdAccess
import org.rundeck.core.auth.access.ProjectIdentifier
import org.rundeck.core.auth.access.Singleton
import rundeck.Project

@CompileStatic
interface ProjectAdhocAuthorizedAccess
    extends AuthorizedIdAccess<Singleton, AuthorizedProjectAdhoc, ProjectIdentifier> {
}
