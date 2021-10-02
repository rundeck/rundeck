package org.rundeck.app.authorization.domain.execution

import groovy.transform.CompileStatic
import org.rundeck.core.auth.access.Accessor
import org.rundeck.core.auth.access.AuthorizingIdResource
import rundeck.Execution

@CompileStatic
interface AuthorizingExecution extends AuthorizingIdResource<Execution, ExecIdentifier> {
    Accessor<Execution> getReadOrView()
    Accessor<Execution> getKill()
    Accessor<Execution> getKillAs()
}