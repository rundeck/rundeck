package com.dtolabs.rundeck.core.logging;

/**
 * Represents the state of a log file
 **/
public enum LogFileState {
    /** Not found at all */
    NOT_FOUND,
    /** Available  */
    AVAILABLE,
    /**
     * In process of being transferred to local storage
     */
    PENDING,
    /**
     * Error determining state
     */
    ERROR
}
