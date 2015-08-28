package com.dtolabs.rundeck.plugins.scm;

/**
 * Synch state of a job
 */
public interface JobState {
    /**
     * @return the synch state
     */
    SynchState getSynchState();

    /**
     * @return the previous commit info if available
     */
    ScmCommitInfo getCommit();
}
