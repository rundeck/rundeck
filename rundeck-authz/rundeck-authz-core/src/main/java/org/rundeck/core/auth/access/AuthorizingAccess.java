package org.rundeck.core.auth.access;

import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext;

/**
 * Simple authorizing access
 */
public interface AuthorizingAccess {
    /**
     * @return auth context
     */
    UserAndRolesAuthContext getAuthContext();

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
}
