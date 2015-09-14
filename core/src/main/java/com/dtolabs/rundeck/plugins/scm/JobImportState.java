package com.dtolabs.rundeck.plugins.scm;

/**
 * Created by greg on 9/14/15.
 */
public interface JobImportState {
    ImportSynchState getSynchState();

    /**
     * @return the current commit info if available
     */
    ScmCommitInfo getCommit();
}
