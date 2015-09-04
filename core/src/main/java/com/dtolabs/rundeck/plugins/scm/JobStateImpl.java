package com.dtolabs.rundeck.plugins.scm;

/**
 * Created by greg on 9/4/15.
 */
public class JobStateImpl implements JobState {
    private ScmCommitInfo scmCommitInfo;
    private SynchState synchState;

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
}
