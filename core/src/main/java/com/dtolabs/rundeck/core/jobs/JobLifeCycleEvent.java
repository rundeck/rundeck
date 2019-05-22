package com.dtolabs.rundeck.core.jobs;

import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionItem;
import com.dtolabs.rundeck.core.logging.LoggingManager;

public class JobLifeCycleEvent {
    private WorkflowExecutionItem item;
    private StepExecutionContext executionContext;

    public JobLifeCycleEvent(){
    }

    public JobLifeCycleEvent(WorkflowExecutionItem item, StepExecutionContext executionContext) {
        this.item = item;
        this.executionContext = executionContext;
    }

    public WorkflowExecutionItem getItem() {
        return item;
    }

    public void setItem(WorkflowExecutionItem item) {
        this.item = item;
    }

    public StepExecutionContext getExecutionContext() {
        return executionContext;
    }

    public void setExecutionContext(StepExecutionContext executionContext) {
        this.executionContext = executionContext;
    }

}
