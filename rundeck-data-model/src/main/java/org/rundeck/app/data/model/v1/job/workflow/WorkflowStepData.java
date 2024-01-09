package org.rundeck.app.data.model.v1.job.workflow;

import java.util.Map;

/**
 * The definition of a workflow step inside a workflow
 * Primarily a step is defined by the type of plugin the system is to use to
 * execute the step, and the configuration for that plugin
 */
public interface WorkflowStepData {
    /**
     * A step that describes specific error handling for this step
     * @return
     */
    WorkflowStepData getErrorHandler();

    /**
     * A boolean that tells the system to proceed on success
     * @return
     */
    Boolean getKeepgoingOnSuccess();

    /**
     *
     * @return A string that describes this step
     */
    String getDescription();

    /**
     * The configuration data for the plugin defined by this step
     * @return
     */
    Map<String,Object> getConfiguration();

    /**
     * True is this step is a node step, false if it is a workflow step
     * @return
     */
    Boolean getNodeStep();

    /**
     *
     * @return The name of the plugin defined by this step
     */
    String getPluginType();

    /**
     * Configuration data for any plugins that
     * are attached to this workflow step
     * @return A map keyed by the plugin name, and the value being the configuration data for that plugin
     */
    Map<String,Object> getPluginConfig();

    String summarize();
}
