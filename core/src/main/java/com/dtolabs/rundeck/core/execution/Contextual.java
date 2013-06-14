package com.dtolabs.rundeck.core.execution;

import java.util.Map;

/**
 * Provides a context
 */
public interface Contextual {
    /**
     * Return the current  context, or null.
     */
    public Map<String, String> getContext();
}
