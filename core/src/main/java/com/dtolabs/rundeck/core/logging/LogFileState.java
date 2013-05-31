package com.dtolabs.rundeck.core.logging;

/** $INTERFACE is ... User: greg Date: 5/29/13 Time: 2:01 PM */
public enum LogFileState {
    /** Not found at all */
    NOT_FOUND,
    /** Available  */
    AVAILABLE,
    /**
     * In process of being transferred to local storage
     */
    PENDING
}
