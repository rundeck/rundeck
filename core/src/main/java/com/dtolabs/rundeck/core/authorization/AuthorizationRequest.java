package com.dtolabs.rundeck.core.authorization;

import java.util.Map;
import java.util.Set;

/**
 * A request to be evaluated
 */
public interface AuthorizationRequest {
    /**
     * @return resource map
     */
    Map<String, String> getResource();

    /**
     * @return subject
     */
    AclSubject getSubject();

    /**
     * @return action requested
     */
    String getAction();

    /**
     * @return environment context
     */
    Set<Attribute> getEnvironment();
}
