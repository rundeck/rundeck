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
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils
import com.dtolabs.rundeck.core.execution.ExecutionContextImpl
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResult
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionListenerImpl
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepException
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResult
import com.dtolabs.rundeck.core.execution.workflow.steps.node.impl.DefaultScriptFileNodeStepUtils
import com.dtolabs.rundeck.core.execution.workflow.steps.node.impl.ScriptFileNodeStepUtils
import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.util.BasicExecutionLogger
import org.rundeck.core.tasks.*
import org.springframework.beans.factory.annotation.Autowired
import rundeck.services.FrameworkService
import rundeck.services.RDTaskContext

@Plugin(name = ScriptTaskConditionHandler.PROVIDER_NAME, service = TaskPluginTypes.TaskConditionHandler)
class ScriptTaskConditionHandler implements TaskConditionHandler<RDTaskContext> {
    static final String PROVIDER_NAME = "script-handler"

    @Override
    boolean handlesCondition(final TaskCondition condition, final RDTaskContext contextInfo) {
        condition instanceof ScriptTaskCondition
    }
    private ScriptFileNodeStepUtils scriptUtils = new DefaultScriptFileNodeStepUtils();

    @Override
    ConditionCheck checkCondition(
        final RDTaskContext contextInfo,
        final Map taskMap,
        final Map triggerMap,
        final TaskTrigger taskTrigger,
        final TaskCondition condition,
        final TaskManager<RDTaskContext> manager
    ) {
        if (!(condition instanceof ScriptTaskCondition)) {
            throw new IllegalArgumentException("Not a ScriptTaskCondition: $condition")
        }
        ScriptTaskCondition scriptCondition = (ScriptTaskCondition) condition


        def fwk = contextInfo.framework
        def fwkProject = fwk.frameworkProjectMgr.getFrameworkProject(contextInfo.project)
        def nodeName = fwk.getFrameworkNodeName()
        def node = fwkProject.getNodeSet().getNode(nodeName)
        def logger = new BasicExecutionLogger(System.out)
        def data = DataContextUtils.context('trigger', DataContextUtils.stringValueMap(triggerMap, null))
        data = DataContextUtils.addContext('task', DataContextUtils.stringValueMap(taskMap, null), data)
        def stepContext = ExecutionContextImpl.builder().with {
            framework fwk
            singleNodeContext node, true
            frameworkProject contextInfo.project
            executionListener new WorkflowExecutionListenerImpl(null, logger)
            executionLogger logger
            dataContext data
            build()
        }

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
                fwk.executionService,
                true
            )


            return ConditionCheck.result(result.success, [exitCode: result.resultCode])
        } catch (NodeStepException e) {
            return ConditionCheck.result(false, [error: e.message])
        }
    }
}
