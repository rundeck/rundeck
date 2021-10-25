package org.rundeck.app.authorization.domain.execution

import groovy.transform.CompileStatic
import org.rundeck.core.auth.AuthConstants
import org.rundeck.core.auth.access.AccessLevels
import org.rundeck.core.auth.access.Accessor
import org.rundeck.core.auth.access.AuthActions
import org.rundeck.core.auth.access.AuthorizingIdResource
import org.rundeck.core.auth.access.NotFound
import org.rundeck.core.auth.access.UnauthorizedAccess
import rundeck.Execution

@CompileStatic
interface AuthorizingExecution extends AuthorizingIdResource<Execution, ExecIdentifier> {

    final static AuthActions APP_KILL = AccessLevels.or([AuthConstants.ACTION_KILL], AccessLevels.APP_ADMIN)
    final static AuthActions APP_KILLAS = AccessLevels.or([AuthConstants.ACTION_KILLAS], AccessLevels.APP_ADMIN)

    Execution getReadOrView() throws UnauthorizedAccess, NotFound
    Execution getKill() throws UnauthorizedAccess, NotFound
    Execution getKillAs() throws UnauthorizedAccess, NotFound
}