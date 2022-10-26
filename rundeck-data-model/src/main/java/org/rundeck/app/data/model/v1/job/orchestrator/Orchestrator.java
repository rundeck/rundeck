package org.rundeck.app.data.model.v1.job.orchestrator;

import java.util.Map;

public interface Orchestrator {
    String getType();
    Map<String,Object> getConfiguration();
}
