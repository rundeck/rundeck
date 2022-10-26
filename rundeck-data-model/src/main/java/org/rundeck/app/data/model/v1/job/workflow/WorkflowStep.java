package org.rundeck.app.data.model.v1.job.workflow;

import java.util.Map;

public interface WorkflowStep {
    WorkflowStep getErrorHandler();
    Boolean isKeepgoingOnSuccess();
    String getDescription();
    Map<String,Object> getStepConfigData();
    Boolean isNodestep();
    String getPluginType();
    Map<String,Object> getPluginConfiguration();
}
