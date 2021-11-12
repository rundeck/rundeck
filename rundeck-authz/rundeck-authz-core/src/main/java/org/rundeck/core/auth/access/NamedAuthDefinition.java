package org.rundeck.core.auth.access;

import java.util.Map;

/**
 * Defines a set of named auth actions
 */
public interface NamedAuthDefinition {
    /**
     * The group name
     */
    String getName();

    /**
     * The auth action defintions by name
     */
    Map<String, AuthActions> getDefinitions();
}
