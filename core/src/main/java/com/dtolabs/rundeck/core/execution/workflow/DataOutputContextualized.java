package com.dtolabs.rundeck.core.execution.workflow;

import com.dtolabs.rundeck.core.dispatcher.DataContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by greg on 5/26/16.
 */
public class DataOutputContextualized implements OutputContext, HasDataContext {
    private final DataOutput dataOutput = new DataOutput();
    String suffix;

    public DataOutputContextualized(final String suffix) {
        this.suffix = suffix;
    }

    @Override
    public void addOutput(final Map<String, Map<String, String>> data) {
        dataOutput.addOutput(prefixAll(data));
    }

    private Map<String, Map<String, String>> prefixAll(final Map<String, Map<String, String>> data) {
        Map<String, Map<String, String>> data2 = new HashMap<>();
        for (String s : data.keySet()) {
            data2.put(s, prefixMap(data.get(s)));

        }
        return data2;
    }

    @Override
    public void addOutput(final String key, final Map<String, String> data) {
        dataOutput.addOutput(key, prefixMap(data));
    }

    private Map<String, String> prefixMap(final Map<String, String> data) {
        Map<String, String> newmap = new HashMap<>();
        for (String s : data.keySet()) {
            newmap.put(s + suffix, data.get(s));
        }
        return newmap;
    }

    @Override
    public void addOutput(final String key, final String name, final String value) {
        dataOutput.addOutput(key, name + suffix, value);
    }

    @Override
    public DataContext getDataContext() {
        return dataOutput.getDataContext();
    }

    public void copyTo(final OutputContext outputContext) {
        outputContext.addOutput(getDataContext());
    }
}
