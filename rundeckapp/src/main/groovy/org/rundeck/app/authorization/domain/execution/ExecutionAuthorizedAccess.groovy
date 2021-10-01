package org.rundeck.app.authorization.domain.execution

import groovy.transform.CompileStatic
import org.rundeck.core.auth.access.AuthorizedIdAccess

import rundeck.Execution

/**
 *
 */
@CompileStatic
interface ExecutionAuthorizedAccess extends AuthorizedIdAccess<Execution, AuthorizedExecution, ExecIdentifier> {

}