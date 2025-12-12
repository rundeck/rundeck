/*
 * Copyright 2024 Rundeck, Inc. (http://rundeck.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
     * @return The workflow step describing the error handler
     */
    WorkflowStepData getErrorHandler();

    /**
     *
     * @return A boolean that tells the system to proceed on success of an error handler
     */
    Boolean getKeepgoingOnSuccess();

    /**
     *
     * @return A string that describes this step
     */
    String getDescription();

    /**
     *
     * @return The configuration data for the plugin defined by this step
     */
    Map<String,Object> getConfiguration();

    /**
     *
     * @return True is this step is a node step, false if it is a workflow step
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


    String getRunnerNode();
}
