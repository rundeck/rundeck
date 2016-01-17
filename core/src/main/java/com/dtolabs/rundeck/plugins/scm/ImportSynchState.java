package com.dtolabs.rundeck.plugins.scm;

/**
 * SCM Import synch state
 */
public enum ImportSynchState {
    CLEAN, UNKNOWN, REFRESH_NEEDED, IMPORT_NEEDED, DELETE_NEEDED
}
