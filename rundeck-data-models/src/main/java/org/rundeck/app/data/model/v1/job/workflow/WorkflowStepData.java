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

import java.util.Collection;
import java.util.List;
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

    Map toMap();

    /**
     * Get plugin config for a specific type
     * @param type Plugin type
     * @return Plugin config for the type, or null
     */
    default Object getPluginConfigForType(String type) {
        Map<String, Object> config = getPluginConfig();
        return config != null ? config.get(type) : null;
    }

    /**
     * Get plugin config list for a specific type
     * @param type Plugin type
     * @return List of plugin configurations for the type, or null
     */
    default List<?> getPluginConfigListForType(String type) {
        Map<String, Object> config = getPluginConfig();
        if (config == null) {
            return null;
        }
        Object val = config.get(type);
        if (val == null) {
            return null;
        }
        if (val instanceof Collection) {
            return new java.util.ArrayList<>((Collection<?>) val);
        }
        return new java.util.ArrayList<>(List.of(val));
    }

    /**
     * Store plugin configuration for a type
     * @param key Plugin type key
     * @param obj Configuration object
     */
    void storePluginConfigForType(String key, Object obj);

    /**
     * Set the entire plugin config map
     * @param obj Map of plugin configurations
     */
    void setPluginConfig(Map<String, Object> obj);

    /**
     * Return map representation without details (for backward compatibility)
     * Default implementation returns the same as toMap()
     * @return Map representation
     */
    default Map toDescriptionMap() {
        return toMap();
    }
    /**
     * Returns the name of the runner where this workflow step should be executed (the "runner node").
     * it could be a remote runner or local server
     *
     * @return the name of the runner node, or {@code null} if the step should use the default node selection.
     */
    String getRunnerNode();
}
