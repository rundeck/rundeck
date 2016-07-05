package com.dtolabs.rundeck.core.rules;

import java.util.Map;

/**
 * A key/value map used as a state
 */
public interface StateObj {
    /**
     * @return current state
     */
    Map<String, String> getState();

    /**
     * @param key   key
     * @param value vaue
     *
     * @return true if the state contains the exact key and value specified
     */
    boolean hasState(String key, String value);

    /**
     * @param state test state
     *
     * @return true if the state contains all of the key/value state specified
     */
    boolean hasState(StateObj state);
}
