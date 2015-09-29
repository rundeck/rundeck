package com.dtolabs.rundeck.plugins.scm;

/**
 * Created by greg on 9/14/15.
 */
public interface JobImportState {
    /**
     * Get the state of the job for import
     * @return state
     */
    ImportSynchState getSynchState();

    /**
     * @return the last imported commit info if available
     */
    ScmCommitInfo getCommit();
}
