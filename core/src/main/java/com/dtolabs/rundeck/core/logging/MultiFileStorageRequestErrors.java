package com.dtolabs.rundeck.core.logging;

/**
 * Extend MultiFileStorageRequest to provide a way to report errors
 */
public interface MultiFileStorageRequestErrors extends MultiFileStorageRequest {

    /**
     * Called to indicate the given filetype storage failed with a message
     *
     * @param filetype file type
     * @param message failure message
     */
    void storageFailureForFiletype(String filetype, String message);
}
