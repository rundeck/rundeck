package org.rundeck.app.authorization.domain.project

import org.rundeck.core.auth.access.AuthorizingIdResource
import org.rundeck.core.auth.access.ProjectIdentifier
import org.rundeck.core.auth.access.Singleton

interface AuthorizingProjectAdhoc extends AuthorizingIdResource<Singleton, ProjectIdentifier> {

}