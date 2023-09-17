package com.dtolabs.rundeck.server.plugins.jobreference

import com.dtolabs.rundeck.core.execution.StepExecutionItem
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext
import com.dtolabs.rundeck.core.execution.workflow.steps.StepException
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionResult
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutor
import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.jobs.JobExecutionItem
import groovy.transform.CompileStatic

@CompileStatic
@Plugin(name = JobExecutionItem.STEP_EXECUTION_TYPE, service = 'StepExecutor')
class JobReferenceStepExecutor implements StepExecutor {
    StepExecutor executionService

    @Override
    boolean isNodeDispatchStep(final StepExecutionItem item) {
        if (!(item instanceof JobExecutionItem)) {
            throw new IllegalArgumentException("Unsupported item type: " + item.getClass().getName());
        }
        JobExecutionItem jitem = (JobExecutionItem) item;
        return jitem.isNodeStep()
    }

    @Override
    StepExecutionResult executeWorkflowStep(final StepExecutionContext executionContext, final StepExecutionItem item)
        throws StepException {
        return executionService.executeWorkflowStep(executionContext, item)
    }
}
