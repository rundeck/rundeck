package com.dtolabs.rundeck.plugins.scm;

/**
 * Created by greg on 9/4/15.
 */
public class ScmExportSynchStateImpl implements ScmExportSynchState {
    private String message;
    private SynchState state;

    @Override
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public SynchState getState() {
        return state;
    }

    public void setState(SynchState state) {
        this.state = state;
    }
}
