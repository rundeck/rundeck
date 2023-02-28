package com.dtolabs.rundeck.server.plugins.jobreference

import com.dtolabs.rundeck.core.common.INodeEntry
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepException
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionItem
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutor
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResult
import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.jobs.JobExecutionItem
import groovy.transform.CompileStatic

@CompileStatic
@Plugin(name = JobExecutionItem.STEP_EXECUTION_TYPE, service = 'NodeStepExecutor')
class JobReferenceNodeStepExecutor implements NodeStepExecutor {
    NodeStepExecutor executionService
    @Override
    NodeStepResult executeNodeStep(
        final StepExecutionContext context,
        final NodeStepExecutionItem item,
        final INodeEntry node
    ) throws NodeStepException {
        return executionService.executeNodeStep(context, item, node)
    }
}
