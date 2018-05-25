package com.dtolabs.rundeck.core.rules;

import java.util.Map;

/**
 * State that can be modified
 */
public interface MutableStateObj extends StateObj {

    /**
     * Add the state to this state
     *
     * @param values state
     *
     * @return true if this state was changed, false otherwise
     */
    public boolean updateState(StateObj values);

    /**
     * Add the state to this state
     *
     * @param values state
     *
     * @return true if this state was changed, false otherwise
     */
    public boolean updateState(Map<String, String> values);

    /**
     * Add the state to this state
     *
     * @param key   key
     * @param value value
     *
     * @return true if this state was changed, false otherwise
     */
    public boolean updateState(String key, String value);

}
