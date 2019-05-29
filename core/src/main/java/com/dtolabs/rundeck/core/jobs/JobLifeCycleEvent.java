package com.dtolabs.rundeck.core.jobs;

import com.dtolabs.rundeck.core.execution.ExecutionLogger;
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;

import java.util.Map;

public interface JobLifeCycleEvent {

    StepExecutionContext getExecutionContext();
    Map<String, String> getOptions();
    ExecutionLogger getExecutionLogger();
    String getUserName();
    String getExecutionId();

}