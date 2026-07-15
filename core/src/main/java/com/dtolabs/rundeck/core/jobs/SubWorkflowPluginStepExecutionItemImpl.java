package com.dtolabs.rundeck.core.jobs;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.*;
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionItem;

/**
 * Implementation of {@link SubWorkflowExecutionItem} that wraps a plugin step execution item with a sub-workflow.
 * <p>
 * This class is used when a plugin step needs to execute a sub-workflow as part of its execution.
 * It delegates most of its behavior to the underlying {@link PluginStepExecutionItemImpl} while
 * providing access to the associated sub-workflow.
 * </p>
 * <p>
 * This implementation supports the execution of nested workflows, allowing plugin steps to define
 * and execute complex workflow structures dynamically.
 * </p>
 *
 * @see SubWorkflowExecutionItem
 * @see PluginStepExecutionItemImpl
 * @see WorkflowExecutionItem
 */
public class SubWorkflowPluginStepExecutionItemImpl implements SubWorkflowExecutionItem {

    /**
     * The sub-workflow to be executed as part of this step.
     */
    WorkflowExecutionItem subWorkflow;

    /**
     * The underlying plugin step execution item that this sub-workflow wraps.
     */
    PluginStepExecutionItemImpl stepExecutionItem;

    /**
     * Constructs a new SubWorkflowPluginStepExecutionItemImpl with the specified plugin step execution item.
     *
     * @param stepExecutionItem the plugin step execution item to wrap; must not be null
     */
    public SubWorkflowPluginStepExecutionItemImpl(PluginStepExecutionItemImpl stepExecutionItem) {
        this.stepExecutionItem = stepExecutionItem;
    }

    /**
     * Returns the sub-workflow associated with this execution item.
     *
     * @return the sub-workflow to be executed, or null if not set
     */
    @Override
    public WorkflowExecutionItem getSubWorkflow() {
        return subWorkflow;
    }

    /**
     * Returns the original step execution item that this sub-workflow wraps.
     *
     * @return the underlying plugin step execution item
     */
    @Override
    public StepExecutionItem getOriginalStep() {
        return stepExecutionItem;
    }

    /**
     * Indicates whether the workflow should continue executing subsequent steps
     * after this step succeeds.
     *
     * @return true if execution should continue on success, false otherwise
     */
    @Override
    public boolean isKeepgoingOnSuccess() {
        return stepExecutionItem.isKeepgoingOnSuccess();
    }

    /**
     * Returns the type identifier for this step execution item.
     *
     * @return the step execution type constant
     */
    @Override
    public String getType() {
        return STEP_EXECUTION_TYPE;
    }

    /**
     * Returns the display label for this step.
     *
     * @return the label text for this step, or null if not set
     */
    @Override
    public String getLabel() {
        return stepExecutionItem.getLabel();
    }


    /**
     * Sets the sub-workflow to be executed as part of this step.
     *
     * @param subWorkflow the workflow execution item to set
     */
    public void setSubWorkflow(WorkflowExecutionItem subWorkflow) {
        this.subWorkflow = subWorkflow;
    }

    /**
     * Sets the underlying plugin step execution item.
     *
     * @param stepExecutionItem the plugin step execution item to set
     */
    public void setStepExecutionItem(PluginStepExecutionItemImpl stepExecutionItem) {
        this.stepExecutionItem = stepExecutionItem;
    }
}
