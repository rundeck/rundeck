package com.dtolabs.rundeck.core.execution.workflow;

import java.util.Map;

/**
 * Add data to a context
 */
public interface OutputContext {
    void addOutput(Map<String, Map<String, String>> data);

    void addOutput(String key, Map<String, String> data);

    public void addOutput(String key, String name, String value);

}
