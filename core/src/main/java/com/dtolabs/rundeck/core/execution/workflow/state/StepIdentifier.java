package com.dtolabs.rundeck.core.execution.workflow.state;

import java.util.List;

/**
 * Identifies a step in a workflow
 */
public interface StepIdentifier {
    /**
     * Return the stack of step contexts
     * @return
     */
    public List<StepContextId> getContext();
}
