package com.dtolabs.rundeck.server.plugins.runner

import com.dtolabs.rundeck.core.execution.StepExecutionItem
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext
import com.dtolabs.rundeck.core.execution.workflow.steps.StepException
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionResult
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutor
import com.dtolabs.rundeck.core.jobs.SubWorkflowExecutionItem
import com.dtolabs.rundeck.core.plugins.Plugin
import groovy.transform.CompileStatic

@CompileStatic
@Plugin(name = SubWorkflowExecutionItem.STEP_EXECUTION_TYPE, service = 'StepExecutor')
class SubWorkflowWorkflowStepExecutor implements StepExecutor {
    StepExecutor executionService

    @Override
    boolean isNodeDispatchStep(StepExecutionItem item) {
        return false
    }

    @Override
    StepExecutionResult executeWorkflowStep(StepExecutionContext executionContext, StepExecutionItem item) throws StepException {
        return executionService.executeWorkflowStep(executionContext, item)
    }
}
