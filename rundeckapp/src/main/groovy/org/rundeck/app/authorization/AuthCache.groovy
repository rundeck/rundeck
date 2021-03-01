package org.rundeck.app.authorization

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.Decision

/**
 * Cache interface for evaluating auth given an AuthContext
 */
interface AuthCache {
    /**
     * Single auth evaluation
     * @param authContext context
     * @param resource resource
     * @param action action
     * @param project project or null for system context
     * @return decision
     */
    Decision evaluate(
        AuthContext authContext,
        Map<String, String> resource,
        String action,
        String project
    )

    /**
     * Multi resource evaluation
     * @param authContext context
     * @param resources resources
     * @param actions actions
     * @param project project or null for system context
     * @return decisions
     */
    Set<Decision> evaluate(
        AuthContext authContext,
        Set<Map<String, String>> resources,
        Set<String> actions,
        String project
    )
}
