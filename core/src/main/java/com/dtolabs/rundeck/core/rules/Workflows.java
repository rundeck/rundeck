package com.dtolabs.rundeck.core.rules;

/**
 * Created by greg on 5/2/16.
 */
public class Workflows {
    public static final String WORKFLOW_STATE_KEY = "workflow.state";
    public static final String WORKFLOW_STATE_STARTED = "started";
    public static final String WORKFLOW_DONE = "workflow.done";

    public static StateObj getWorkflowEndState() {
        return States.state(WORKFLOW_DONE, Boolean.TRUE.toString());
    }

    public static StateObj getWorkflowStartState() {
        return States.state(
                States.state(WORKFLOW_STATE_KEY, WORKFLOW_STATE_STARTED)
        );
    }


    public static WorkflowSystemBuilder builder()
    {
        return WorkflowEngineBuilder.builder();
    }
}
