package org.rundeck.core.auth.access;

import javax.security.auth.Subject;

/**
 * Simple access authorizer
 *
 * @param <A>
 */
public interface AccessAuthorizer<A extends AuthorizingAccess> {
    /**
     * @param subject authorization
     * @return authorizing resource
     */
    A getAuthorizingResource(Subject subject);
}
