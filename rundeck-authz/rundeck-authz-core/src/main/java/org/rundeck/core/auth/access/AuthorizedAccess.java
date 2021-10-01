package org.rundeck.core.auth.access;

import javax.security.auth.Subject;

/**
 * Accessor for an authorized resource of a certain type without ID (singleton)
 *
 * @param <T>
 * @param <A>
 */
public interface AuthorizedAccess<T, A extends AuthorizedResource<T>> {
    A accessResource(Subject subject);
}
