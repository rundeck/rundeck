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
import org.rundeck.core.tasks.ActionFailed
import org.rundeck.core.tasks.TaskAction
import org.rundeck.core.tasks.TaskActionHandler
import org.rundeck.core.tasks.TaskManager
import org.rundeck.core.tasks.TaskPluginTypes
import org.rundeck.core.tasks.TaskTrigger
import rundeck.services.RDTaskContext

@Plugin(
    name = MultiActionHandler.PROVIDER_NAME,
    service = TaskPluginTypes.TaskActionHandler
)
class MultiActionHandler implements TaskActionHandler<RDTaskContext> {
    static final String PROVIDER_NAME = 'MultiActionHandler'


    @Override
    Map performTaskAction(
        final RDTaskContext contextInfo,
        final Map triggerMap,
        final Map userData,
        Map conditionData,
        final TaskTrigger taskTrigger,
        final TaskAction action,
        TaskManager<RDTaskContext> manager
    ) throws ActionFailed {
        if (action instanceof MultipleTaskAction) {


            MultipleTaskAction multi = (MultipleTaskAction) action


            def resultlist = []
            for (TaskAction subaction : multi.actions) {

                resultlist << manager.performTaskAction(
                    subaction,
                    contextInfo,
                    triggerMap,
                    userData,
                    conditionData,
                    taskTrigger
                )
            }

            return [list: resultlist]
        } else if (action instanceof TestEmbedTaskAction) {
            TestEmbedTaskAction test = (TestEmbedTaskAction) action
            return [result: test.actions.stringvalue]
        }
    }

    @Override
    boolean handlesAction(final TaskAction action, final RDTaskContext contextInfo) {
        action instanceof MultipleTaskAction || action instanceof TestEmbedTaskAction
    }
}
