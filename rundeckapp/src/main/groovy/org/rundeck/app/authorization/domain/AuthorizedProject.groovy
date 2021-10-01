package org.rundeck.app.authorization.domain

import org.rundeck.app.authorization.AuthorizedResource
import org.rundeck.app.authorization.NotFound
import org.rundeck.app.authorization.UnauthorizedAccess
import rundeck.Project

interface AuthorizedProject extends AuthorizedResource<Project> {

    Project requireDeleteExecution() throws UnauthorizedAccess, NotFound

    boolean hasDeleteExecution() throws NotFound
}