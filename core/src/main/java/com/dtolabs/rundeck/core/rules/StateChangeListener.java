package com.dtolabs.rundeck.core.rules;

/**
 * Created by greg on 4/28/16.
 */
public interface StateChangeListener {
    void stateChanged(StateObj stateObj);
}
