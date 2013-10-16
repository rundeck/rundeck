package com.dtolabs.rundeck.core.execution.workflow.state;

import java.util.List;

/**
 * $INTERFACE is ... User: greg Date: 10/15/13 Time: 2:54 PM
 */
public interface StepIdentifier {
    /**
     * Return the stack of step contexts
     * @return
     */
    public List<Integer> getContext();
}
