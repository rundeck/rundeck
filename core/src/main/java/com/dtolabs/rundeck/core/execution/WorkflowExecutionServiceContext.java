package com.dtolabs.rundeck.core.execution;

import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionResult;

/**
 * Context for workflow execution service
 */
public interface WorkflowExecutionServiceContext extends ServiceContext<WorkflowExecutionResult> {
    StepExecutionContext getContext();

    WorkflowExecutionResult getResult();
}
