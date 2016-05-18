package com.dtolabs.rundeck.core.rules;


/**
 * Defines a condition and a new state based on the old state
 */
public interface Rule extends Condition {
    /**
     * @param stateObj input state
     *
     * @return new state entries if the condition is successful
     */
    StateObj evaluate(StateObj stateObj);
}
