package org.rundeck.app.components.jobs;

import java.util.Map;

/**
 * A named set of metadata
 */
public interface ComponentMeta {
    String getName();

    Map<String, Object> getData();

    static ComponentMeta with(String name, Map<String, Object> data) {
        return new ComponentMeta() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public Map<String, Object> getData() {
                return data;
            }
        };
    }
}
