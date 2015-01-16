package com.dtolabs.rundeck.core.execution.workflow.state;

import java.util.List;

/**
 * Identifies a step in a workflow
 */
public interface StepIdentifier extends Comparable<StepIdentifier>{
    /**
     * @return the stack of step contexts
     */
    public List<StepContextId> getContext();
}
