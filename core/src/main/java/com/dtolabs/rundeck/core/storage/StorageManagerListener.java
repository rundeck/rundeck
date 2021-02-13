package com.dtolabs.rundeck.core.storage;

/**
 * Listener for changes to StorageManager resources
 */
public interface StorageManagerListener {
    void resourceCreated(String path);

    void resourceDeleted(String path);

    void resourceModified(String path);
}
