package com.dtolabs.rundeck.core.rules;

import java.util.Map;

/**
 * Created by greg on 4/28/16.
 */
public interface MutableStateObj extends StateObj {

    public boolean updateState(StateObj values);

    public boolean updateState(Map<String, String> values);

    public boolean updateState(String key, String value);

}
