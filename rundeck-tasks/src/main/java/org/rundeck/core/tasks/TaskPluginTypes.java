/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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

package org.rundeck.core.tasks;

import org.rundeck.core.plugins.PluginType;

import java.util.HashMap;
import java.util.Map;

/**
 * Declares plugin types for Tasks
 */
public class TaskPluginTypes implements PluginType {
    public static final String TaskTrigger        = "TaskTrigger";
    public static final String TaskTriggerHandler = "TaskTriggerHandler";
    public static final String TaskAction         = "TaskAction";
    public static final String TaskActionHandler  = "TaskActionHandler";
    /**
     * Map of Service name to Class
     */
    public static final Map<String, Class> TYPES;

    static {
        HashMap<String, Class> map = new HashMap<>();
        map.put(TaskPluginTypes.TaskTrigger, TaskTrigger.class);
        map.put(TaskPluginTypes.TaskTriggerHandler, TaskTriggerHandler.class);
        map.put(TaskPluginTypes.TaskAction, TaskAction.class);
        map.put(TaskPluginTypes.TaskActionHandler, TaskActionHandler.class);
        TYPES = map;
    }

    public Class<?> getPluginType(String name) {
        return TYPES.get(name);
    }
}
