package com.dtolabs.rundeck.core.rules;


/**
 * Defines a condition and a new state based on the old state
 */
public interface Rule extends Condition {
    StateObj evaluate(StateObj stateObj);
}
