package org.rundeck.app.authorization.domain.system

import org.rundeck.core.auth.access.Accessor
import org.rundeck.core.auth.access.AuthorizedResource
import org.rundeck.core.auth.access.Singleton

interface AuthorizedSystem extends AuthorizedResource<Singleton> {

    Accessor<Singleton> getConfigure()

    Accessor<Singleton> getOpsEnableExecution()

    Accessor<Singleton> getOpsDisableExecution()

}