package com.dtolabs.rundeck.core.execution.workflow;

import com.dtolabs.rundeck.core.dispatcher.DataContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by greg on 5/26/16.
 */
public class DataOutputContextualized implements OutputContext, HasDataContext {
    private final DataOutput dataOutput = new DataOutput();
    String prefix;

    public DataOutputContextualized(final String prefix) {
        this.prefix = prefix;
    }

    @Override
    public void addOutput(final Map<String, Map<String, String>> data) {
        dataOutput.addOutput(prefixAll(data));
    }

    private Map<String, Map<String, String>> prefixAll(final Map<String, Map<String, String>> data) {
        Map<String, Map<String, String>> data2 = new HashMap<>();
        for (String s : data.keySet()) {
            data2.put(prefix + s, data.get(s));
        }
        return data2;
    }

    @Override
    public void addOutput(final String key, final Map<String, String> data) {
        dataOutput.addOutput(prefix + key, data);
    }

    @Override
    public void addOutput(final String key, final String name, final String value) {
        dataOutput.addOutput(prefix + key, name, value);
    }

    @Override
    public DataContext getDataContext() {
        return dataOutput.getDataContext();
    }
}
