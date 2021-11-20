package org.rundeck.core.auth.access;

import org.rundeck.core.auth.app.RundeckAccess;

/**
 * Authorized access to a Resource of a certain type and identity
 *
 * @param <T>
 */
public interface AuthorizingIdResource<T, ID>
        extends AuthorizingResource<T>
{
    /**
     * @return true if resource exists
     */
    boolean exists();

    /**
     * @return identifier
     */
    ID getIdentifier();
}
