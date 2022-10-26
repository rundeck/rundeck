package org.rundeck.app.data.model.v1.job.workflow;

import java.util.List;

public interface Workflow {
    Integer getThreadcount();
    Boolean isKeepgoing();
    List<WorkflowStep> getCommands();
    String getStrategy();
    String getPluginConfig();
}
