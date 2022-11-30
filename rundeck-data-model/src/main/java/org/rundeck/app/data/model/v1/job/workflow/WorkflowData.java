package org.rundeck.app.data.model.v1.job.workflow;

import java.util.List;
import java.util.Map;

public interface WorkflowData {
    Integer getThreadcount();
    Boolean getKeepgoing();
    List<WorkflowStepData> getSteps();
    String getStrategy();
    Map<String,Object> getPluginConfigMap();
}
