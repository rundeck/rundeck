package org.rundeck.core.auth.access;

import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext;

/**
 * Authorized access to a Resource of a certain type without identity (singleton)
 *
 * @param <T>
 */
public interface AuthorizingResource<T> {
    /**
     * @return auth context
     */
    UserAndRolesAuthContext getAuthContext();

    /**
     * @param actions auth actions
     * @return authorized accessor
     */
    Accessor<T> accessor(AuthActions actions);

    /**
     * Access object
     *
     * @param actions auth actions
     * @throws UnauthorizedAccess if unauthorized
     * @throws NotFound           if not found
     */
    T access(AuthActions actions) throws UnauthorizedAccess, NotFound;

    /**
     * Check authorization
     *
     * @param actions auth actions
     * @throws UnauthorizedAccess if unauthorized
     * @throws NotFound           if not found
     */
    void authorize(AuthActions actions) throws UnauthorizedAccess, NotFound;

    /**
     * test authorization
     *
     * @param actions auth actions
     * @return true if authorized, false otherwise
     * @throws NotFound if not found
     */
    boolean isAuthorized(AuthActions actions) throws NotFound;

    /**
     * @return locator
     */
    Locator<T> getLocator();

    /**
     * READ access
     *
     * @return resource
     */
    T getRead() throws UnauthorizedAccess, NotFound;

    /**
     * APP_ADMIN access
     *
     * @return resource
     */
    T getAppAdmin() throws UnauthorizedAccess, NotFound;

    /**
     * OPS_ADMIN access
     *
     * @return resource
     */
    T getOpsAdmin() throws UnauthorizedAccess, NotFound;
}
