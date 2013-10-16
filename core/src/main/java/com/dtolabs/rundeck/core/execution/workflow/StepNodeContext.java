package com.dtolabs.rundeck.core.execution.workflow;

import java.util.List;

/**
 * Reports the current step and node context, where step is maintained as a stack
 */
public interface StepNodeContext<NODE, STEP> {
    /**
     * Return the current node
     * @return
     */
    NODE getCurrentNode();

    /**
     * Return the stack for the step context
     * @return
     */
    List<STEP> getCurrentContext();
}
