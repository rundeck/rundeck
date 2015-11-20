package com.dtolabs.rundeck.core.logging;

import java.util.Set;

/**
 * A set of storage files accessible by type
 */
public interface MultiFileStorageSet {
    /**
     * @return the set of file types available
     */
    Set<String> getAvailableFiletypes();

    /**
     * @param filetype file type string
     *
     * @return storage file for the given type, or null if not available
     */
    StorageFile getStorageFile(String filetype);
}
