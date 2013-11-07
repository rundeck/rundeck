package com.dtolabs.rundeck.core.execution.workflow.state;

import java.util.List;

/**
 * $INTERFACE is ... User: greg Date: 10/15/13 Time: 3:54 PM
 */
public class StepIdentifierImpl implements StepIdentifier {
    private List<StepContextId> context;

    public StepIdentifierImpl(List<StepContextId> context) {
        this.context = context;
    }

    public List<StepContextId> getContext() {
        return context;
    }

    @Override
    public String toString() {
        return "StepIdentifierImpl{" +
                "context=" + context +
                '}';
    }
}
