package org.rundeck.core.auth.app.type;

import org.rundeck.core.auth.access.AuthorizingIdResource;
import org.rundeck.core.auth.access.Singleton;

/**
 * A resource type in a project context
 */
public interface AuthorizingProjectType
        extends AuthorizingIdResource<Singleton, ProjectTypeIdentifier>
{
}
