package org.rundeck.core.auth.app;

import org.rundeck.core.auth.access.*;
import org.rundeck.core.auth.app.type.*;

import javax.security.auth.Subject;

/**
 * Authorizing access to resources
 */
public interface DomainAuthorizer {
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
     * @return true if authorized
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

    /**
     * Authorizing system
     *
     * @param subject subject
     * @return authorizing system
     */
    AuthorizingSystem system(Subject subject);

    /**
     * Authorizing application resource type
     * @param subject
     * @param resolver
     * @return
     */
    AuthorizingAppType applicationType(Subject subject, ResIdResolver resolver);
    AuthorizingAppType applicationType(Subject subject, String type);

    /**
     * Authorizing project resource type
     * @param subject
     * @param resolver
     * @return
     */
    AuthorizingProjectType projectType(Subject subject, ResIdResolver resolver);
    AuthorizingProjectType projectType(Subject subject, String project, String type);



    AuthorizingProjectAdhoc adhoc(Subject subject, ResIdResolver resolver);
    AuthorizingProjectAdhoc adhoc(Subject subject, String project);

    AuthorizingProjectAcl projectAcl(Subject subject, ResIdResolver resolver);
    AuthorizingProjectAcl projectAcl(Subject subject, String project);
}
