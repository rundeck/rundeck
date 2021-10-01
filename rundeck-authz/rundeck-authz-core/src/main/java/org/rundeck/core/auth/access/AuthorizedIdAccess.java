package org.rundeck.core.auth.access;

import javax.security.auth.Subject;

/**
 * Accessor for an authorized resource of a certain type and ID
 *
 * @param <T> resource type
 * @param <A> authorized resource type
 * @param <I> ID type
 */
public interface AuthorizedIdAccess<T, A extends AuthorizedIdResource<T, I>, I> {
    A accessResource(Subject subject, I identifier);

}
