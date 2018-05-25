package com.dtolabs.rundeck.core.rules;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by greg on 4/28/16.
 */
public class DataState implements ListenableMutableStateObj {
    Map<String, String> state;
    List<StateChangeListener> listeners = new ArrayList<>();

    DataState() {
        this(new HashMap<String, String>());
    }

    DataState(StateObj context) {
        state = new HashMap<>(context.getState());
    }
    DataState(Map<String, String> context) {
        state = new HashMap<>(context);
    }

    DataState(final String key, final String val) {
        state = new HashMap<>();
        state.put(key, val);
    }

    @Override
    public boolean updateState(final StateObj values) {
        if (updateStateInt(values)) {
            stateChanged();
            return true;
        }
        return false;
    }

    private boolean updateStateInt(final StateObj values) {
        return updateStateInt(values.getState());
    }

    public boolean updateState(Map<String, String> values) {
        if (updateStateInt(values)) {
            stateChanged();
            return true;
        }
        return false;
    }

    private boolean updateStateInt(Map<String, String> values) {
        boolean changed = false;
        if (null == values || values.size() < 1) {
            return false;
        }
        for (String s : values.keySet()) {
            boolean val = updateStateInt(s, values.get(s));
            changed = val || changed;
        }
        return changed;
    }

    public boolean updateState(String key, String value) {
        if (updateStateInt(key, value)) {
            stateChanged();
            return true;
        }
        return false;
    }


    private boolean updateStateInt(String key, String value) {
        if (null != value) {
            return !value.equals(state.put(key, value));
        } else {
            return state.remove(key) != null;
        }
    }

    @Override
    public Map<String, String> getState() {
        return state;
    }

    @Override
    public boolean hasState(final String key, final String value) {
        return value != null ? value.equals(state.get(key)) : state.get(key) == null;
    }

    @Override
    public boolean hasState(final StateObj state) {
        return FluentIterable.from(state.getState().keySet()).allMatch(
                new Predicate<String>() {
                    @Override
                    public boolean apply(final String input) {
                        return hasState(input, state.getState().get(input));
                    }
                }
        );
    }

    @Override
    public void addListener(final StateChangeListener listener) {
        listeners.add(listener);
    }

    private void stateChanged() {
        for (StateChangeListener listener : listeners) {
            listener.stateChanged(this);
        }
    }

    @Override
    public String toString() {
        return "DataState{" +
               "state=" + state +
               '}';
    }
}
