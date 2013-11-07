package com.dtolabs.rundeck.core.execution.workflow.state;

/**
 * Identifies the context of a step execution
 */
public interface StepContextId {
    public int getStep();
    public StepAspect getAspect();
}
