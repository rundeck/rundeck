package org.rundeck.app.authorization.domain

import groovy.transform.CompileStatic
import org.rundeck.app.authorization.AuthorizedResource
import org.rundeck.app.authorization.NotFound
import org.rundeck.app.authorization.UnauthorizedAccess
import rundeck.Execution

@CompileStatic
interface AuthorizedExecution extends AuthorizedResource<Execution> {


    Execution requireReadOrView() throws UnauthorizedAccess, NotFound

    boolean canReadOrView() throws NotFound

    Execution requireKill() throws UnauthorizedAccess, NotFound

    boolean canKill() throws NotFound
}