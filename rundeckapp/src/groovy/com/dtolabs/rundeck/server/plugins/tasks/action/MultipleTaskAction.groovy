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

package com.dtolabs.rundeck.server.plugins.tasks.action

import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.plugins.descriptions.EmbeddedPluginProperty
import com.dtolabs.rundeck.plugins.descriptions.EmbeddedTypeProperty
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.server.plugins.tasks.PluginBaseMetaTrait
import groovy.transform.ToString
import org.rundeck.core.tasks.TaskAction
import org.rundeck.core.tasks.TaskPluginTypes


@Plugin(name = MultipleTaskAction.PROVIDER_NAME, service = TaskPluginTypes.TaskAction)
@PluginDescription(title = 'Multiple Actions', description = 'Execute multiple actions')
@ToString(includeNames = true, includeFields = true)
class MultipleTaskAction implements TaskAction, PluginBaseMetaTrait {
    static final String PROVIDER_NAME = "multiple"


//    @PluginProperty(required = true, title = 'Actions(TODO)', description = 'Actions to run.')
//    @EmbeddedPluginProperty(type = TaskAction)
//    List<TaskAction> actions

    @PluginProperty(required = true, title = 'Action 1', description = 'Test action')
    @EmbeddedPluginProperty(type = TaskAction)
    TaskAction stubAction

}
