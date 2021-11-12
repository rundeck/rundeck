package org.rundeck.core.auth.app;

/**
 * Untyped authorization request
 */
public interface NamedAuthRequest {
    /**
     * @return Auth group name
     */
    String getAuthGroup();

    /**
     * @return auth name
     */
    String getNamedAuth();

    /**
     * @return optional description
     */
    String getDescription();
}
