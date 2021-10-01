package org.rundeck.app.authorization.domain

import groovy.transform.CompileStatic
import org.rundeck.app.authorization.AuthorizedAccess
import rundeck.Execution

@CompileStatic
interface ExecutionAuthorizedAccess extends AuthorizedAccess<Execution, AuthorizedExecution, ExecIdentifier> {

}