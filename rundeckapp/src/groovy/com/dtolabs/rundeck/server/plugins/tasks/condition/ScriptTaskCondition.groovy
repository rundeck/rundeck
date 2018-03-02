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
import com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.descriptions.RenderingOption
import com.dtolabs.rundeck.plugins.descriptions.RenderingOptions
import com.dtolabs.rundeck.plugins.descriptions.SelectValues
import com.dtolabs.rundeck.server.plugins.tasks.PluginBaseMetaTrait
import org.rundeck.core.tasks.TaskCondition
import org.rundeck.core.tasks.TaskPluginTypes
import org.rundeck.core.tasks.TaskTrigger

@Plugin(name = ScriptTaskCondition.PROVIDER_NAME, service = TaskPluginTypes.TaskCondition)
@PluginDescription(title = "Script", description = "Run a script and require success")
class ScriptTaskCondition implements TaskCondition, PluginBaseMetaTrait {
    static final String PROVIDER_NAME = "script"

    @PluginProperty(title = "Language", description = "Script interpreter language", required = true)
    @SelectValues(values = ['shell', 'groovy'])
    String lang

    @PluginProperty(title = "Script", description = "Script to Execute", required = true)
    @RenderingOptions([
        @RenderingOption(key = StringRenderingConstants.DISPLAY_TYPE_KEY, value = "CODE"),
        @RenderingOption(key = StringRenderingConstants.CODE_SYNTAX_MODE, value = "bash"),
        @RenderingOption(key = StringRenderingConstants.CODE_SYNTAX_SELECTABLE, value = "true")
    ])
    String script

    @Override
    boolean appliesToTrigger(final TaskTrigger trigger) {
        return true
    }


    Map meta = [glyphicon: 'file']
}
