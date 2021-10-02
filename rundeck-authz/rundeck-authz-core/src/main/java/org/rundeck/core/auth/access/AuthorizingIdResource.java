package org.rundeck.core.auth.access;

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

    Accessor<T> getDelete();
}
