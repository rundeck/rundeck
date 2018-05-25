package com.dtolabs.rundeck.core.execution.workflow;

import java.util.Map;

/**
 * Add data to a context
 */
public interface OutputContext {
    /**
     * Add data
     *
     * @param data
     */
    void addOutput(Map<String, Map<String, String>> data);

    /**
     * Add data
     *
     * @param group
     * @param data
     */
    void addOutput(String group, Map<String, String> data);

    /**
     * Add a single group/name/value
     *
     * @param group
     * @param name
     * @param value
     */
    void addOutput(String group, String name, String value);

}
