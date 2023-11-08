package org.rundeck.app.components.jobs;

import java.util.Map;

/**
 * A named set of metadata for a job
 */
public interface JobMeta {
    String getName();

    Map<String, Object> getData();

    static JobMeta with(String name, Map<String, Object> data) {
        return new JobMeta() {
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
