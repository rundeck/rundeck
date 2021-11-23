package org.rundeck.core.auth.app;

import org.rundeck.core.auth.access.*;
import org.rundeck.core.auth.app.type.*;

import javax.security.auth.Subject;

/**
 * Authorizing access to base resource types
 */
public interface BaseTypedRequestAuthorizer
        extends TypedRequestAuthorizer
{
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
