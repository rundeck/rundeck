package com.dtolabs.rundeck.core.jobs;

import com.dtolabs.rundeck.core.execution.HandlerExecutionItem;
import com.dtolabs.rundeck.core.execution.StepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionItem;

public interface SubWorkflowExecutionItem extends HandlerExecutionItem, StepExecutionItem {
    public final static String STEP_EXECUTION_TYPE = "subworkflow-workflow-step";

    WorkflowExecutionItem getSubWorkflow();

    StepExecutionItem getOriginalStep();

}
