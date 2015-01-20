package com.dtolabs.rundeck.core.execution.workflow;

import java.util.List;

/**
 * Reports the current step and node context, where step is maintained as a stack
 */
public interface StepNodeContext<NODE, STEP> {
    /**
     * @return the current node
     */
    NODE getCurrentNode();

    /**
     * @return the stack for the step context
     */
    List<STEP> getCurrentContext();
}
