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

package com.dtolabs.rundeck.server.plugins.tasks.condition

import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.plugins.descriptions.EmbeddedPluginProperty
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import org.rundeck.core.tasks.TaskCondition
import org.rundeck.core.tasks.TaskPluginTypes
import org.rundeck.core.tasks.TaskTrigger

@Plugin(name = InvertTaskCondition.PROVIDER_NAME, service = TaskPluginTypes.TaskCondition)
@PluginDescription(title = "Invert Condition", description = "Invert the result of another condition")
class InvertTaskCondition implements TaskCondition {
    static final String PROVIDER_NAME = "not"

    @PluginProperty(title = 'Condition', description = 'Condition to invert')
    @EmbeddedPluginProperty
    TaskCondition condition

    @Override
    boolean appliesToTrigger(final TaskTrigger trigger) {
        true
    }
}
