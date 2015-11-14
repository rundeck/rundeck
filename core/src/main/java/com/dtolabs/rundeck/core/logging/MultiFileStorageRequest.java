package com.dtolabs.rundeck.core.logging;

/**
 * Represents a request to store files, allowing callback of whether file storage has been performed successfully
 */
public interface MultiFileStorageRequest extends MultiFileStorageSet {
    /**
     * Called to indicate the given filetype storage was completed
     *
     * @param filetype file type
     */
    void storageResultForFiletype(String filetype, boolean success);
}
