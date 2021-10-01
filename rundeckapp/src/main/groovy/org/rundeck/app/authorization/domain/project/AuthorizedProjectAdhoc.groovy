package org.rundeck.app.authorization.domain.project

import org.rundeck.core.auth.access.AuthorizedIdResource
import org.rundeck.core.auth.access.ProjectIdentifier
import org.rundeck.core.auth.access.Singleton

interface AuthorizedProjectAdhoc extends AuthorizedIdResource<Singleton, ProjectIdentifier> {

}