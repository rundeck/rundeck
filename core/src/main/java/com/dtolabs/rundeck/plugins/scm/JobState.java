package com.dtolabs.rundeck.plugins.scm;

import com.dtolabs.rundeck.core.plugins.views.Action;

import java.util.List;

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

    /**
     * @return List of actions available for the job based on the state
     */
    List<Action> getActions();
}
