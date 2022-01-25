package org.rundeck.core.auth.access;

import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext;
import org.rundeck.core.auth.app.NamedAuthRequest;

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
     * Check authorization
     *
     * @param namedAuthRequest named auth check
     * @throws UnauthorizedAccess if unauthorized
     * @throws NotFound           if not found
     */
    void authorizeNamed(NamedAuthRequest namedAuthRequest) throws UnauthorizedAccess, NotFound;

    /**
     * Check authorization
     *
     * @param namedAuthRequest named auth check
     * @throws NotFound           if not found
     */
    boolean isAuthorized(NamedAuthRequest namedAuthRequest) throws  NotFound;

    /**
     * test authorization
     *
     * @param actions auth actions
     * @return true if authorized, false otherwise
     * @throws NotFound if not found
     */
    boolean isAuthorized(AuthActions actions) throws NotFound;
}
