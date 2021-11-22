package org.rundeck.app.authorization.domain.execution

import groovy.transform.CompileStatic
import org.rundeck.core.auth.access.ProjectResIdentifier
import org.rundeck.core.auth.access.AuthorizingIdResource
import rundeck.Execution

/**
 * Authorizing access to Execution domain object. nb: currently tied to Domain class, but should be migrated
 * to common java lib (rundeck-core) once there is an interface type that can be used instead of the Domain class.
 */
@CompileStatic
interface AuthorizingExecution extends AuthorizingIdResource<Execution, ProjectResIdentifier> {

}