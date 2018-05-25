package com.dtolabs.rundeck.core.rules;

/**
 * Created by greg on 4/28/16.
 */
public interface ListenableMutableStateObj extends MutableStateObj {
    void addListener(StateChangeListener listener);
}
