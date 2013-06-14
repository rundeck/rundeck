package com.dtolabs.rundeck.plugins.logging;

import com.dtolabs.rundeck.core.logging.LogFileState;
import com.dtolabs.rundeck.core.logging.LogFileStorage;
import com.dtolabs.rundeck.core.logging.LogFileStorageException;

import java.util.Map;

/**
 * Plugin interface for Log file storage
 */
public interface LogFileStoragePlugin extends LogFileStorage {
    /**
     * Initializes the plugin with contextual data
     * @param context
     */
    public void initialize(Map<String, ? extends Object> context);

    /**
     * Returns true if the file is available, false otherwise
     * @return
     * @throws LogFileStorageException if there is an error determining the availability
     */
    public boolean isAvailable() throws LogFileStorageException;
}
