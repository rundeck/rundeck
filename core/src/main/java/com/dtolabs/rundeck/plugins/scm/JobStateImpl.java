package com.dtolabs.rundeck.plugins.scm;

import com.dtolabs.rundeck.core.plugins.views.Action;

import java.util.List;

/**
 * Created by greg on 9/4/15.
 */
public class JobStateImpl implements JobState {
    private ScmCommitInfo scmCommitInfo;
    private SynchState synchState;
    private List<Action> actions;

    public ScmCommitInfo getCommit() {
        return scmCommitInfo;
    }

    public void setCommit(ScmCommitInfo scmCommitInfo) {
        this.scmCommitInfo = scmCommitInfo;
    }

    @Override
    public SynchState getSynchState() {
        return synchState;
    }

    public void setSynchState(SynchState synchState) {
        this.synchState = synchState;
    }

    @Override
    public List<Action> getActions() {
        return actions;
    }

    public void setActions(List<Action> actions) {
        this.actions = actions;
    }
}
