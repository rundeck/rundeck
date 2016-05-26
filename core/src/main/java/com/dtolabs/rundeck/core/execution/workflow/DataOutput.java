package com.dtolabs.rundeck.core.execution.workflow;

import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by greg on 5/25/16.
 */
public class DataOutput implements OutputContext {
    private Map<String, Map<String, String>> outputContext;

    public DataOutput() {
        outputContext = DataContextUtils.context();
    }

    @Override
    public void addOutput(Map<String, Map<String, String>> data) {
        DataContextUtils.merge(outputContext, data);
    }

    @Override
    public void addOutput(String key, Map<String, String> data) {
        DataContextUtils.addContext(key, data, outputContext);
    }

    public void addOutput(final String key, final String name, final String value) {
        HashMap<String, String> data = new HashMap<>();
        data.put(name, value);
        DataContextUtils.addContext(key, data, outputContext);
    }

    public Map<String, Map<String, String>> getOutputContext() {
        return outputContext;
    }
}
