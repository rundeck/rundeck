package com.dtolabs.rundeck.plugins.logging;

import com.dtolabs.rundeck.core.logging.LogFileState;
import com.dtolabs.rundeck.core.logging.LogFileStorage;

import java.util.Map;

/**
 *
 */
public interface LogFileStoragePlugin extends LogFileStorage {
    /**
     * Initializes the plugin with contextual data
     * @param context
     */
    public void initialize(Map<String, ? extends Object> context);

    /**
     * Returns the state of the log file storage
     * @return
     */
    public LogFileState getState();
}
