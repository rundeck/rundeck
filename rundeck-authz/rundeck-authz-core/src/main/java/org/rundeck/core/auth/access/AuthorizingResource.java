package org.rundeck.core.auth.access;

import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext;

/**
 * Authorized access to a Resource of a certain type without identity (singleton)
 *
 * @param <T>
 */
public interface AuthorizingResource<T>
        extends AuthorizingAccess
{

    /**
     * Access resource with required authorization
     *
     * @param actions auth actions
     * @throws UnauthorizedAccess if unauthorized
     * @throws NotFound           if not found
     */
    T access(AuthActions actions) throws UnauthorizedAccess, NotFound;

    /**
     * Access resource without authorization checks
     *
     * @return resource
     * @throws NotFound if not found
     */
    T getResource() throws NotFound;
}
