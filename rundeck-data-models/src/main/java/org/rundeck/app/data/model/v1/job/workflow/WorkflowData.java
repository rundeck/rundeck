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
 * Describes the workflow for a job
 */
public interface WorkflowData {
    /**
     *
     * @return Thread count to execute this workflow
     */
    Integer getThreadcount();

    /**
     *
     * @return Defines the workflow behavior when an error is encountered
     */
    Boolean getKeepgoing();

    /**
     *
     * @return The steps to run in the workflow
     */
    List<WorkflowStepData> getSteps();

    List<WorkflowStepData> getCommands();

    /**
     *
     * @return Defines the workflow strategy plugin to apply for this workflow
     */
    String getStrategy();

    /**
     *
     * @return Configuration data for the plugins attached to the workflow
     */
    Map<String,Object> getPluginConfigMap();

    /**
     *
     * @return Map representation of this workflow
     */
    Map<String, Object> toMap();

    /**
     * Get the config for a plugin type
     * @param type Plugin type
     * @return available config data, or null
     */
    default Object getPluginConfigData(String type) {
        Map<String, Object> map = getPluginConfigMap();
        return map != null ? map.get(type) : null;
    }

    /**
     * Get the config for a plugin type, wraps the value as a list if it is not a collection
     * @param type Plugin type
     * @return available config data, as a List, or null
     */
    default Collection getPluginConfigDataList(String type) {
        Map<String, Object> map = getPluginConfigMap();
        if (map == null) {
            return null;
        }
        Object val = map.get(type);
        if (val == null) {
            return null;
        }
        if (val instanceof Collection) {
            return new java.util.ArrayList<>((Collection<?>) val);
        }
        return new java.util.ArrayList<>(List.of(val));
    }

    /**
     * Get the config for a plugin type expecting a map, and an entry in the map
     * @param type Plugin type
     * @param name Plugin name
     * @return available map data or empty map
     */
    default Map<String, Object> getPluginConfigData(String type, String name) {
        Map<String, Object> map = getPluginConfigMap();
        if (map == null) {
            return new java.util.HashMap<>();
        }
        Object typeObj = map.get(type);
        if (typeObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> typeMap = (Map<String, Object>) typeObj;
            Object value = typeMap.get(name);
            if (value != null) {
                return new java.util.HashMap<>(Map.of(name, value));
            }
        }
        return new java.util.HashMap<>();
    }

    /**
     * Set plugin config data for a specific type
     * @param type Plugin type
     * @param data Configuration data
     */
    void setPluginConfigData(String type, Object data);

    /**
     * Set plugin config data for a specific type and name
     * @param type Plugin type
     * @param name Plugin name
     * @param data Configuration data
     */
    void setPluginConfigData(String type, String name, Object data);

    /**
     * Set the entire plugin config map
     * @param obj Map of plugin configurations
     */
    void setPluginConfigMap(Map<String, Object> obj);

    /**
     * Validate plugin config map structure
     * @return true if valid, false otherwise
     */
    default boolean validatePluginConfigMap() {
        Map<String, Object> configMap = getPluginConfigMap();
        if (configMap == null) {
            return true;
        }
        Object workflowStrategyConfigObj = configMap.get("WorkflowStrategy");
        if (workflowStrategyConfigObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> workflowStrategyConfig = (Map<String, Object>) workflowStrategyConfigObj;
            String strategy = getStrategy();
            if (strategy != null && workflowStrategyConfig.containsKey(strategy)) {
                Object strategyConfigObj = workflowStrategyConfig.get(strategy);
                if (strategyConfigObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> strategyConfig = (Map<String, Object>) strategyConfigObj;
                    // Validate that the map structure is correct
                    return strategyConfig.size() != 1 ||
                           !strategyConfig.keySet().stream().anyMatch(strategy::equals);
                }
            }
        }
        return true;
    }
}
