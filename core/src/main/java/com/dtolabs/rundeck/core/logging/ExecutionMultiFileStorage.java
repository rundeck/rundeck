package com.dtolabs.rundeck.core.logging;

import java.io.IOException;

/**
 * Allows storing multiple files with a single method call
 */
public interface ExecutionMultiFileStorage {

    /**
     * Store some or all of the available files
     *
     * @param files available file set
     *
     * @throws java.io.IOException                                            if an IO error occurs
     * @throws com.dtolabs.rundeck.core.logging.ExecutionFileStorageException if other errors occur
     */
    void storeMultiple(MultiFileStorageRequest files) throws IOException, ExecutionFileStorageException;
}
