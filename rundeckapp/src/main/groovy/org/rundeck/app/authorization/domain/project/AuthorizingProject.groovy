package org.rundeck.app.authorization.domain.project


import org.rundeck.core.auth.access.AuthorizingIdResource
import org.rundeck.core.auth.access.NotFound
import org.rundeck.core.auth.access.ProjectIdentifier
import org.rundeck.core.auth.access.UnauthorizedAccess
import rundeck.Project

interface AuthorizingProject extends AuthorizingIdResource<Project, ProjectIdentifier> {
    Project getConfigure() throws UnauthorizedAccess, NotFound
    Project getDeleteExecution() throws UnauthorizedAccess, NotFound
    Project getExport() throws UnauthorizedAccess, NotFound
    Project getImport() throws UnauthorizedAccess, NotFound
    Project getPromote() throws UnauthorizedAccess, NotFound
}