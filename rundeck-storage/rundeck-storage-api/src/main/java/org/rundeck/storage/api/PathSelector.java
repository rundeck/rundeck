package org.rundeck.storage.api;

/**
 * Specifies if it applies to a particular resource path
 */
public interface PathSelector {
    /**
     * @return true if the given path matches
     *
     * @param path path
     *
     */
    boolean matchesPath(Path path);
}
