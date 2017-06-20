package com.dtolabs.rundeck.core.execution;

/**
 * Created by greg on 5/11/16.
 */
public abstract class BaseExecutionItem implements StepExecutionItem{
    private String label;

    @Override
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
