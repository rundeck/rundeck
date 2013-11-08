package com.dtolabs.rundeck.core.execution.workflow.state;

/**
 * Aspect of a step
 */
public enum StepAspect {
    Main,
    ErrorHandler;

    public boolean isMain(){
        return this==Main;
    }
}
