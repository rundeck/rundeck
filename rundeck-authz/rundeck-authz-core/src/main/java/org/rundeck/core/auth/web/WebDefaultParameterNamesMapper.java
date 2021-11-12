package org.rundeck.core.auth.web;

import java.util.Map;

/**
 * Maps "resource type" name to parameter name for the resource's ID.
 */
public interface WebDefaultParameterNamesMapper {
    /**
     * @return Map of "type" name to parameter name
     */
    Map<String, String> getWebDefaultParameterNames();
}
