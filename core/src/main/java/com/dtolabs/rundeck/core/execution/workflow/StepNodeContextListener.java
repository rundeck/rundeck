package com.dtolabs.rundeck.core.execution.workflow;

/**
 * Listens to context changes, where context can contain a node or step, or overall context.
 */
public interface StepNodeContextListener<NODE, STEP> {
    /**
     * Indicates context begins
     */
    void beginContext();

    /**
     * Indicates context has finished
     */
    void finishContext();

    /**
     * Enter a step context
     * @param step step
     */
    void beginStepContext(STEP step);

    /**
     * finish a step context
     */
    void finishStepContext();

    /**
     * Enter a node context
     * @param node node
     */
    void beginNodeContext(NODE node);

    /**
     * Finish a node context
     */
    void finishNodeContext();
}
