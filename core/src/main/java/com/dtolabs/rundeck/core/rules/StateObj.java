package com.dtolabs.rundeck.core.rules;

import java.util.Map;

/**
 * Created by greg on 4/28/16.
 */
public interface StateObj {
    Map<String, String> getState();

    boolean hasState(String key, String value);
    boolean hasState(StateObj state);
}
