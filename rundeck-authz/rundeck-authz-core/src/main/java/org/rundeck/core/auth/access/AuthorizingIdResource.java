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

    ID getIdentifier();

    /**
     * Authorize with {@link RundeckAccess.General#APP_DELETE}
     * @return object
     * @throws UnauthorizedAccess if unauthorized
     * @throws NotFound if not found
     */
    T getDelete() throws UnauthorizedAccess, NotFound;
}
