package com.dtolabs.rundeck.core.rules;

import java.util.UUID;

/**
 * Created by greg on 5/2/16.
 */
public class Workflows {
    public static final String WORKFLOW_STATE_KEY = "workflow.state";
    public static final String WORKFLOW_STATE_ID_KEY = "workflow.id";
    public static final String WORKFLOW_STATE_STARTED = "started";
    public static final String WORKFLOW_DONE = "workflow.done";

    public static StateObj getWorkflowEndState() {
        return States.state(WORKFLOW_DONE, Boolean.TRUE.toString());
    }

    public static StateObj getWorkflowStartState() {
        return States.state(
                States.state(
                        WORKFLOW_STATE_KEY, WORKFLOW_STATE_STARTED
                )
        );
    }

    /**
     * @return a state with a {@link #WORKFLOW_STATE_ID_KEY} entry with a unique value
     */
    public static StateObj getNewWorkflowState() {
        return States.state(WORKFLOW_STATE_ID_KEY, UUID.randomUUID().toString());
    }


    public static WorkflowSystemBuilder builder()
    {
        return WorkflowEngineBuilder.builder();
    }
}
