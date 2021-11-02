package org.rundeck.core.auth.access;

import javax.security.auth.Subject;

/**
 * Accessor for an authorized resource of a certain type without ID (singleton)
 *
 * @param <T>
 * @param <A>
 */
public interface ResourceAuthorizer<T, A extends AuthorizingResource<T>>
        extends AccessAuthorizer<A>
{

}
