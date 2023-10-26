package com.dtolabs.rundeck.core.execution.workflow.state;

import com.dtolabs.rundeck.core.execution.ExecutionReference;
import com.dtolabs.rundeck.core.execution.logstorage.ExecutionFileLoader;

public interface WorkflowStateDataLoader {
    ExecutionFileLoader loadWorkflowStateData(ExecutionReference executionReference, boolean performLoad);
}
