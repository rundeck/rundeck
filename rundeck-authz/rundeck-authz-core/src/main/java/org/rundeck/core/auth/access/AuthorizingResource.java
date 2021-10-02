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

    Accessor<T> access(AuthActions actions);

    /**
     * @return locator
     */
    Locator<T> getLocator();

    /**
     * READ access
     *
     * @return resource
     */
    Accessor<T> getRead();

    /**
     * APP_ADMIN access
     *
     * @return resource
     */
    Accessor<T> getAppAdmin();

    /**
     * OPS_ADMIN access
     *
     * @return resource
     */
    Accessor<T> getOpsAdmin();
}
