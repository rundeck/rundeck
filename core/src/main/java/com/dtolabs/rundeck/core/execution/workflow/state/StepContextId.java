package com.dtolabs.rundeck.core.execution.workflow.state;

import java.util.Map;

/**
 * Identifies the context of a step execution
 */
public interface StepContextId extends Comparable<StepContextId>{
    public int getStep();
    public StepAspect getAspect();
    public Map<String,String> getParams();
}
