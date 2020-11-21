package com.dtolabs.rundeck.core.authorization;

import java.util.Map;
import java.util.Set;

/**
 * Evaluate action authorization for a context
 */
public interface AuthEvaluator {
    Decision evaluate(
            AuthContext authContext,
            Map<String, String> resource,
            String action,
            String project
    );

    Set<Decision> evaluate(
            AuthContext authContext,
            Set<Map<String, String>> resources,
            Set<String> actions,
            String project
    );
}
