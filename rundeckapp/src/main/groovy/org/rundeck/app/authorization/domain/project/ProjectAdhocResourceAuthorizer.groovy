package org.rundeck.app.authorization.domain.project

import groovy.transform.CompileStatic
import org.rundeck.core.auth.access.ResourceIdAuthorizer
import org.rundeck.core.auth.access.ProjectIdentifier
import org.rundeck.core.auth.access.Singleton

@CompileStatic
interface ProjectAdhocResourceAuthorizer
    extends ResourceIdAuthorizer<Singleton, AuthorizingProjectAdhoc, ProjectIdentifier> {
}
