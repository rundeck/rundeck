package com.dtolabs.rundeck.plugins.scm;

import com.dtolabs.rundeck.core.plugins.views.Action;

import java.util.List;

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

    /**
     * @return Actions available
     */
    List<Action> getActions();
}
