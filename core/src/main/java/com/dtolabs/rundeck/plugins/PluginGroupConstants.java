/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
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

package com.dtolabs.rundeck.plugins;

/**
 * Constants for plugin grouping metadata.
 * Use with @PluginMetadata to categorize plugins for UI grouping.
 *
 * Example:
 * <pre>
 * @Plugin(name = "my-plugin", service = ServiceNameConstants.WorkflowStep)
 * @PluginDescription(title = "My Plugin", description = "Does stuff")
 * @PluginMetadata(key = PluginGroupConstants.PLUGIN_GROUP_KEY,
 *                 value = "MyGroup")
 * public class MyPlugin implements StepPlugin { ... }
 * </pre>
 */
public class PluginGroupConstants {

    /**
     * The metadata key for plugin grouping.
     * Use this with @PluginMetadata annotation.
     */
    public static final String PLUGIN_GROUP_KEY = "groupBy";

    /** Default group for plugins without explicit groupBy metadata */
    public static final String GROUP_OTHER = "Other";
}
