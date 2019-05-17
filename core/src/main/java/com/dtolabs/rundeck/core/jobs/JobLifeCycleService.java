package com.dtolabs.rundeck.core.jobs;

import com.dtolabs.rundeck.core.execution.JobLifeCycleException;
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionItem;
import com.dtolabs.rundeck.core.logging.LoggingManager;

public interface JobLifeCycleService {

    boolean onBeforeJobStart(WorkflowExecutionItem item, StepExecutionContext executionContext,
                             LoggingManager workflowLogManager) throws JobLifeCycleException;

}
