package org.rundeck.app.data.model.v1.job.workflow;

import java.util.List;
import java.util.Map;

/**
 * Describes the workflow for a job
 */
public interface WorkflowData {
    /**
     * Thread count to execute this workflow
     * @return
     */
    Integer getThreadcount();

    /**
     * Defines the workflow behavior when an error is encountered
     * @return
     */
    Boolean getKeepgoing();

    /**
     * The steps to run in the workflow
     * @return
     */
    List<WorkflowStepData> getSteps();

    /**
     * Defines the workflow strategy plugin to apply for this workflow
     * @return
     */
    String getStrategy();

    /**
     * Configuration data for the plugins attaches to the workflow
     * @return
     */
    Map<String,Object> getPluginConfigMap();
}
