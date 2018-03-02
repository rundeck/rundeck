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

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.execution.ExecutionContextImpl
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResult
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepException
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResult
import com.dtolabs.rundeck.core.execution.workflow.steps.node.impl.DefaultScriptFileNodeStepUtils
import com.dtolabs.rundeck.core.execution.workflow.steps.node.impl.ScriptFileNodeStepUtils
import com.dtolabs.rundeck.core.plugins.Plugin
import org.rundeck.core.tasks.*
import org.springframework.beans.factory.annotation.Autowired
import rundeck.services.FrameworkService
import rundeck.services.RDTaskContext

@Plugin(name = ScriptTaskConditionHandler.PROVIDER_NAME, service = TaskPluginTypes.TaskConditionHandler)
class ScriptTaskConditionHandler implements TaskConditionHandler<RDTaskContext> {
    static final String PROVIDER_NAME = "script-handler"

    @Autowired
    FrameworkService frameworkService


    @Override
    boolean handlesCondition(final TaskCondition condition, final RDTaskContext contextInfo) {
        condition instanceof ScriptTaskCondition
    }
    private ScriptFileNodeStepUtils scriptUtils = new DefaultScriptFileNodeStepUtils();

    @Override
    ConditionCheck checkCondition(
        final RDTaskContext contextInfo,
        final Map triggerMap,
        final TaskTrigger taskTrigger,
        final TaskCondition condition
    ) {
        if (!(condition instanceof ScriptTaskCondition)) {
            throw new IllegalArgumentException("Not a ScriptTaskCondition: $condition")
        }
        ScriptTaskCondition scriptCondition = (ScriptTaskCondition) condition


        def framework = frameworkService.getRundeckFramework()
        def fwkProject = frameworkService.getFrameworkProject(contextInfo.project)
        def nodeName = framework.getFrameworkNodeName()
        def node = fwkProject.getNodeSet().getNode(nodeName)
        def stepContext = ExecutionContextImpl.builder().
            framework(framework).
            singleNodeContext(node, true).
            frameworkProject(contextInfo.project).
            build()

        try {
            NodeExecutorResult result = scriptUtils.executeScriptFile(
                stepContext,
                node,
                scriptCondition.script,
                null,
                null,
                null,
                null, //TODO: args,interpreter,quoted..
                null,
                false,
                executionService,
                true
            )

            return ConditionCheck.result(result.success, [exitCode: result.resultCode])
        } catch (NodeStepException e) {
            return ConditionCheck.result(false, [error: e.message])
        }
    }
}
