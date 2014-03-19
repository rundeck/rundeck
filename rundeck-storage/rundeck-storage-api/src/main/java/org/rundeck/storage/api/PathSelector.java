package org.rundeck.storage.api;

/**
 * Specifies if it applies to a particular resource path
 */
public interface PathSelector {
    /**
     * returns true if the given path should be managed by this handler
     *
     * @param path
     *
     * @return
     */
    boolean matchesPath(Path path);
}
