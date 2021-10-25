package org.rundeck.app.authorization.domain.project

import org.rundeck.core.auth.AuthConstants
import org.rundeck.core.auth.access.AccessLevels
import org.rundeck.core.auth.access.AuthActions
import org.rundeck.core.auth.access.AuthorizingIdResource
import org.rundeck.core.auth.access.NotFound
import org.rundeck.core.auth.access.ProjectIdentifier
import org.rundeck.core.auth.access.UnauthorizedAccess
import rundeck.Project

interface AuthorizingProject extends AuthorizingIdResource<Project, ProjectIdentifier> {
    public static final AuthActions APP_DELETE_EXECUTION =
        AccessLevels.action(AuthConstants.ACTION_DELETE_EXECUTION)
                    .or(AccessLevels.APP_ADMIN)

    public static final AuthActions APP_EXPORT =
        AccessLevels.action(AuthConstants.ACTION_EXPORT)
                    .or(AccessLevels.APP_ADMIN)

    Project getDeleteExecution() throws UnauthorizedAccess, NotFound
    Project getExport() throws UnauthorizedAccess, NotFound
}