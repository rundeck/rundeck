package com.dtolabs.rundeck.core.rules;

import com.dtolabs.rundeck.core.Constants;
import com.dtolabs.rundeck.plugins.PluginLogger;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Created by greg on 5/5/16.
 */
public class StateLogger implements MutableStateObj {
    MutableStateObj state;
    private Consumer<String> listener;

    public StateLogger(
            final MutableStateObj state,
            final Consumer<String> listener
    )
    {
        this.state = state;
        this.listener = listener;
    }

    @Override
    public Map<String, String> getState() {
        return state.getState();
    }

    @Override
    public boolean updateState(final StateObj values) {
        boolean result = state.updateState(values);
        listener.accept("Update conditional state: " + values.getState());
        return result;
    }

    @Override
    public boolean updateState(final Map<String, String> values) {
        boolean result = state.updateState(values);
        listener.accept("Update conditional state: " + values);
        return result;
    }

    @Override
    public boolean updateState(final String key, final String value) {
        boolean result = state.updateState(key, value);
        listener.accept("Update conditional state: " + key + "=" + value);
        return result;
    }

    @Override
    public boolean hasState(final String key, final String value) {
        return state.hasState(key, value);
    }

    @Override
    public boolean hasState(final StateObj statex) {
        return state.hasState(statex);
    }

    @Override
    public String toString() {
        return "StateLogger{" +
               "state=" + state +
               '}';
    }
}
