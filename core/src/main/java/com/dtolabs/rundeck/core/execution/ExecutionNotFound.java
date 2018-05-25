package com.dtolabs.rundeck.core.execution;


public class ExecutionNotFound extends Exception{

    private String executionId;
    private String project;


    public ExecutionNotFound(String message, String executionId, String project){
        super(message);
        this.executionId = executionId;
        this.project = project;
    }

    public String getExecutionId() {
        return executionId;
    }

    public String getProject() {
        return project;
    }
}
