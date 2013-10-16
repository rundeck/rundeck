package com.dtolabs.rundeck.core.execution.workflow.state;

import java.util.List;

/**
 * $INTERFACE is ... User: greg Date: 10/15/13 Time: 3:54 PM
 */
public class StepIdentifierImpl implements StepIdentifier {
    private List<Integer> context;

    public StepIdentifierImpl(List<Integer> context) {
        this.context = context;
    }

    public List<Integer> getContext() {
        return context;
    }

    @Override
    public String toString() {
        return "StepIdentifierImpl{" +
                "context=" + context +
                '}';
    }
}
