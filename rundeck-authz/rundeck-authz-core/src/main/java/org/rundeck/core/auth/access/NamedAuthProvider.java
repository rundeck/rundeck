package org.rundeck.core.auth.access;


/**
 * Provides named auth definition maps
 */
public interface NamedAuthProvider {
    /**
     * Get Auth by group and name
     *
     * @param group
     * @param name
     */
    AuthActions getNamedAuth(String group, String name);
}
