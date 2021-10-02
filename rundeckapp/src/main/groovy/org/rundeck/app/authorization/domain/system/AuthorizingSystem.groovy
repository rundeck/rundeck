package org.rundeck.app.authorization.domain.system

import groovy.transform.CompileStatic
import org.rundeck.core.auth.access.Accessor
import org.rundeck.core.auth.access.AuthorizingResource
import org.rundeck.core.auth.access.Singleton
@CompileStatic
interface AuthorizingSystem extends AuthorizingResource<Singleton> {

    Accessor<Singleton> getConfigure()

    Accessor<Singleton> getOpsEnableExecution()

    Accessor<Singleton> getOpsDisableExecution()

}