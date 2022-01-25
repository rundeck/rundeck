package org.rundeck.core.auth.app;

import org.rundeck.core.auth.access.*;

import javax.security.auth.Subject;

/**
 * Authorize typed requests, or provide AuthorizingAccess
 */
public interface TypedRequestAuthorizer {
    /**
     * Test authorized access
     *
     * @param subject  subject
     * @param resolver id resolver
     * @param request  auth request
     * @return true if authorized
     * @throws MissingParameter if required id parameter is missing
     */
    boolean isAuthorized(
            Subject subject,
            ResIdResolver resolver,
            TypedNamedAuthRequest request
    )
            throws MissingParameter;

    /**
     * authorize access
     *
     * @param subject  subject
     * @param resolver id resolver
     * @param request  auth request
     * @throws MissingParameter if required id parameter is missing
     */
    void authorize(
            Subject subject,
            ResIdResolver resolver,
            TypedNamedAuthRequest request
    )
            throws UnauthorizedAccess, NotFound, MissingParameter;

    /**
     * Get AuthorizingAccess for a resource type
     *
     * @param subject  subject
     * @param resolver id resolver
     * @param type     resource type
     * @return authorizing access
     * @throws MissingParameter if required id param is missing
     */
    AuthorizingAccess getAuthorizingAccess(Subject subject, ResIdResolver resolver, String type)
            throws MissingParameter;
}
