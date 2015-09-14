package com.dtolabs.rundeck.plugins.scm;

/**
 * Export Synch state of all jobs
 */
public interface ScmExportSynchState {
    SynchState getState();

    String getMessage();
}
