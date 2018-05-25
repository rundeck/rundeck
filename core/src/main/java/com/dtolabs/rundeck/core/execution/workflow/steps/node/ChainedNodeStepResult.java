package com.dtolabs.rundeck.core.execution.workflow.steps.node;

/**
 * A node step result that wraps another
 */
public interface ChainedNodeStepResult extends NodeStepResult{
    NodeStepResult getOriginal();
}
