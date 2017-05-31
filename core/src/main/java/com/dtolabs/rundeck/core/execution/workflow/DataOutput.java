package com.dtolabs.rundeck.core.execution.workflow;

import com.dtolabs.rundeck.core.dispatcher.ContextView;
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import com.dtolabs.rundeck.core.data.SharedDataContextUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by greg on 5/25/16.
 */
public class DataOutput implements ReadableSharedContext {
    private WFSharedContext outputContext;
    private ContextView defaultView;

    public DataOutput(ContextView defaultView) {
        this(defaultView, SharedDataContextUtils.sharedContext());
    }

    public DataOutput(ContextView defaultView, WFSharedContext context) {
        outputContext = context;
        this.defaultView = defaultView;
    }

    @Override
    public void addOutput(
            final ContextView view, final Map<String, Map<String, String>> data
    )
    {

        outputContext.merge(defaultView, DataContextUtils.context(data));
    }

    @Override
    public void addOutput(final ContextView view, final String key, final Map<String, String> data) {

        outputContext.merge(view, DataContextUtils.context(DataContextUtils.addContext(key, data, null)));
    }

    @Override
    public void addOutput(final ContextView view, final String key, final String name, final String value) {

        HashMap<String, String> data = new HashMap<>();
        data.put(name, value);
        addOutput(view, key, data);
    }

    @Override
    public void addOutput(Map<String, Map<String, String>> data) {
        addOutput(defaultView, data);
    }

    @Override
    public void addOutput(String group, Map<String, String> data) {
        addOutput(defaultView, group, data);
    }

    public void addOutput(final String group, final String name, final String value) {
        addOutput(defaultView, group, name, value);
    }

    public WFSharedContext getSharedContext() {
        return outputContext;
    }
}
