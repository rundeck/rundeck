package org.rundeck.storage.api;

/**
 * Address within a tree
 */
public interface Path {
    /**
     * @return the path as a string
     */
    public String getPath();

    /**
     * @return the last component of the path
     */
    public String getName();
}
