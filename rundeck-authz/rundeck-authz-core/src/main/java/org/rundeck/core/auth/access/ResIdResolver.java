package org.rundeck.core.auth.access;

import org.rundeck.core.auth.access.MissingParameter;

import java.util.Optional;

/**
 * Resolves ID value for resource types
 */
public interface ResIdResolver {
    /**
     * @param type type
     * @return ID value for type
     * @throws MissingParameter if ID value cannot be resolved
     */
    public String idForType(String type) throws MissingParameter;

    /**
     * @param type resource type
     * @return ID value for type
     */
    public Optional<String> idForTypeOptional(String type);
}
