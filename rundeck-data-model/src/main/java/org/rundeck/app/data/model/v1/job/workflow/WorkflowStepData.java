package org.rundeck.app.data.model.v1.job.workflow;

import java.util.Map;

public interface WorkflowStepData {
    WorkflowStepData getErrorHandler();
    Boolean getKeepgoingOnSuccess();
    String getDescription();
    Map<String,Object> getConfiguration();
    Boolean getNodeStep();
    String getPluginType();
    Map<String,Object> getPluginConfig();
}
