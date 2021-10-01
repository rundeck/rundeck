package org.rundeck.app.authorization.domain.execution

import groovy.transform.CompileStatic
import org.rundeck.core.auth.access.Accessor
import org.rundeck.core.auth.access.AuthorizedIdResource
import org.rundeck.core.auth.access.NotFound
import org.rundeck.core.auth.access.UnauthorizedAccess
import rundeck.Execution

@CompileStatic
interface AuthorizedExecution extends AuthorizedIdResource<Execution, ExecIdentifier> {


    Accessor<Execution> getReadOrView()

    Accessor<Execution> getKill()
    Accessor<Execution> getKillAs()
}