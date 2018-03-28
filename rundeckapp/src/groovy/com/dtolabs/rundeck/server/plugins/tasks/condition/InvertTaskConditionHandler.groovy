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
import org.rundeck.core.tasks.ConditionCheck
import org.rundeck.core.tasks.TaskCondition
import org.rundeck.core.tasks.TaskConditionHandler
import org.rundeck.core.tasks.TaskException
import org.rundeck.core.tasks.TaskManager
import org.rundeck.core.tasks.TaskPluginTypes
import org.rundeck.core.tasks.TaskTrigger
import rundeck.services.RDTaskContext

@Plugin(name = InvertTaskConditionHandler.PROVIDER_NAME, service = TaskPluginTypes.TaskConditionHandler)
class InvertTaskConditionHandler implements TaskConditionHandler<RDTaskContext> {
    static final String PROVIDER_NAME = 'InvertTaskConditionHandler'

    @Override
    boolean handlesCondition(final TaskCondition condition, final RDTaskContext contextInfo) {
        condition instanceof InvertTaskCondition
    }

    @Override
    ConditionCheck checkCondition(
        final RDTaskContext contextInfo,
        final Map taskMap,
        final Map triggerMap,
        final TaskTrigger taskTrigger,
        final TaskCondition condition,
        TaskManager<RDTaskContext> taskManager
    ) throws TaskException {
        InvertTaskCondition invert = (InvertTaskCondition) condition

        ConditionCheck result = taskManager.checkCondition(
            invert.condition,
            contextInfo,
            taskMap,
            triggerMap,
            taskTrigger
        )

        ConditionCheck.result(!result.conditionMet, result.conditionData)
    }
}
