package org.rundeck.core.auth.access;

import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext;

/**
 * Authorized access to a Resource of a certain type and identity
 *
 * @param <T>
 */
public interface AuthorizedIdResource<T, ID> extends AuthorizedResource<T> {

    /**
     * @return true if resource exists
     */
    boolean exists();

    ID getIdentifier();

    Accessor<T> getDelete();
}
