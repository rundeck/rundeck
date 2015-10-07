package com.dtolabs.rundeck.plugins.scm;

/**
 * Import Synch state of all jobs
 */
public interface ScmImportSynchState {
    ImportSynchState getState();

    String getMessage();
}
