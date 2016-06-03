package com.dtolabs.rundeck.core.execution.workflow;

import com.dtolabs.rundeck.core.dispatcher.DataContext;
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import com.dtolabs.rundeck.core.dispatcher.MutableDataContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by greg on 5/25/16.
 */
public class DataOutput implements ReadableOutputContext {
    private MutableDataContext outputContext;

    public DataOutput() {
        outputContext = DataContextUtils.context();
    }

    @Override
    public void addOutput(Map<String, Map<String, String>> data) {
        outputContext.merge(DataContextUtils.context(data));
    }

    @Override
    public void addOutput(String key, Map<String, String> data) {
        outputContext.merge(DataContextUtils.context(DataContextUtils.addContext(key, data, null)));
    }

    public void addOutput(final String key, final String name, final String value) {
        HashMap<String, String> data = new HashMap<>();
        data.put(name, value);
        addOutput(key,data);
    }

    public DataContext getDataContext() {
        return outputContext;
    }
}
