package org.rundeck.app.authorization.domain.system

import org.rundeck.core.auth.access.Accessor
import org.rundeck.core.auth.access.AuthorizedResource

interface AuthorizedSystem extends AuthorizedResource<Void> {

    Accessor<Void> getConfigure()

    Accessor<Void> getOpsEnableExecution()

    Accessor<Void> getOpsDisableExecution()

}