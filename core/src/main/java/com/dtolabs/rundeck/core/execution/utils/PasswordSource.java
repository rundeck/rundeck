package com.dtolabs.rundeck.core.execution.utils;

/**
* Created by greg on 3/19/15.
*/
public interface PasswordSource {
    byte[] getPassword();
    void clear();
}
