package org.rundeck.app.auth;

import com.dtolabs.rundeck.core.authorization.Authorization;
import com.dtolabs.rundeck.core.authorization.UserAndRoles;

/**
 * Provides Authorizations for subjects
 */
public interface AuthManager {
    /**
     * @param subject subject
     * @return System level authorization object for the user and roles
     */
    Authorization getAuthorizationForSubject(UserAndRoles subject);

    /**
     * @param subject subject
     * @param project project name
     * @return System and Project authorization object for the user and roles, and specific project
     */
    Authorization getProjectAuthorizationForSubject(UserAndRoles subject, String project);
}
