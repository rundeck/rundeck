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
    public static final AuthActions APP_CONFIGURE =
        AccessLevels.action(AuthConstants.ACTION_CONFIGURE)
                    .or(AccessLevels.APP_ADMIN)

    public static final AuthActions APP_DELETE_EXECUTION =
        AccessLevels.action(AuthConstants.ACTION_DELETE_EXECUTION)
                    .or(AccessLevels.APP_ADMIN)

    public static final AuthActions APP_EXPORT =
        AccessLevels.action(AuthConstants.ACTION_EXPORT)
                    .or(AccessLevels.APP_ADMIN)

    public static final AuthActions APP_IMPORT =
        AccessLevels.action(AuthConstants.ACTION_IMPORT)
                    .or(AccessLevels.APP_ADMIN)

    public static final AuthActions APP_PROMOTE =
        AccessLevels.action(AuthConstants.ACTION_PROMOTE)
                    .or(AccessLevels.APP_ADMIN)

    Project getConfigure() throws UnauthorizedAccess, NotFound
    Project getDeleteExecution() throws UnauthorizedAccess, NotFound
    Project getExport() throws UnauthorizedAccess, NotFound
    Project getImport() throws UnauthorizedAccess, NotFound
    Project getPromote() throws UnauthorizedAccess, NotFound
}