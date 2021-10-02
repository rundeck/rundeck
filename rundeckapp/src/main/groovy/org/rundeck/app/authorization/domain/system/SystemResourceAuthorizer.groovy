package org.rundeck.app.authorization.domain.system

import groovy.transform.CompileStatic
import org.rundeck.core.auth.access.ResourceAuthorizer
import org.rundeck.core.auth.access.Singleton

@CompileStatic
interface SystemResourceAuthorizer extends ResourceAuthorizer<Singleton, AuthorizingSystem> {

}