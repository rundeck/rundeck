package org.rundeck.app.auth;

import org.rundeck.app.auth.types.AuthorizingProject;
import org.rundeck.core.auth.access.ResIdResolver;
import org.rundeck.core.auth.app.BaseTypedRequestAuthorizer;

import javax.security.auth.Subject;

/**
 * Authorizing access to Core resource types
 */
public interface CoreTypedRequestAuthorizer
        extends BaseTypedRequestAuthorizer
{

    AuthorizingProject project(Subject subject, ResIdResolver resolver);
    AuthorizingProject project(Subject subject, String project);
}
