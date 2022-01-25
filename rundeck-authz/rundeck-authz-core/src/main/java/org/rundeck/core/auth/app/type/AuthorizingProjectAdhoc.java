package org.rundeck.core.auth.app.type;

import org.rundeck.core.auth.access.AuthorizingIdResource;
import org.rundeck.core.auth.access.ProjectIdentifier;
import org.rundeck.core.auth.access.Singleton;

/**
 * Authorizing interface to singleton Adhoc type within a project
 */
public interface AuthorizingProjectAdhoc
        extends AuthorizingIdResource<Singleton, ProjectIdentifier>
{
}
