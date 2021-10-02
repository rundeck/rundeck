package org.rundeck.app.authorization.domain.execution

import groovy.transform.CompileStatic
import org.rundeck.core.auth.access.ResourceIdAuthorizer

import rundeck.Execution

/**
 *
 */
@CompileStatic
interface ExecutionResourceAuthorizer extends ResourceIdAuthorizer<Execution, AuthorizingExecution, ExecIdentifier> {

}