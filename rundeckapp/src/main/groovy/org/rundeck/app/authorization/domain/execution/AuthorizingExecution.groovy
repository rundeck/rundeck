package org.rundeck.app.authorization.domain.execution

import groovy.transform.CompileStatic
import org.rundeck.core.auth.access.AuthorizingIdResource
import org.rundeck.core.auth.access.NotFound
import org.rundeck.core.auth.access.UnauthorizedAccess
import rundeck.Execution

@CompileStatic
interface AuthorizingExecution extends AuthorizingIdResource<Execution, ExecIdentifier> {

    Execution getReadOrView() throws UnauthorizedAccess, NotFound
    Execution getKill() throws UnauthorizedAccess, NotFound
    Execution getKillAs() throws UnauthorizedAccess, NotFound
}