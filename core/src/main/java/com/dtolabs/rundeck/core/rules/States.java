package com.dtolabs.rundeck.core.rules;

import java.util.Map;

/**
 * Created by greg on 5/2/16.
 */
public class States {

    public static MutableStateObj mutable() {
        return new DataState();
    }

    public static MutableStateObj mutable(StateObj context) {
        return new DataState(context);
    }

    public static StateObj state(StateObj context) {
        return new DataState(context);
    }

    /**
     * merge multiple state objects in order
     *
     * @param states states
     *
     * @return
     */
    public static StateObj state(StateObj... states) {
        MutableStateObj mutable = mutable();
        for (StateObj state : states) {
            mutable.updateState(state);
        }
        return state(mutable.getState());
    }

    public static StateObj state(Map<String, String> context) {
        return new DataState(context);
    }

    public static StateObj state(final String key, final String val) {
        return new DataState(key, val);
    }

    public static MutableStateObj mutable(Map<String, String> context) {
        return new DataState(context);
    }

    public static MutableStateObj mutable(final String key, final String val) {
        return new DataState(key, val);
    }
}
