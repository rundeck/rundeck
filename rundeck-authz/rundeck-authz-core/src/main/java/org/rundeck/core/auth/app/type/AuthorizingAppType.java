package org.rundeck.core.auth.app.type;

import org.rundeck.core.auth.access.AuthorizingIdResource;
import org.rundeck.core.auth.access.Singleton;

/**
 * A resource type int the application context
 */
public interface AuthorizingAppType
        extends AuthorizingIdResource<Singleton, String>
{
}
