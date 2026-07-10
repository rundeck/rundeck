package com.dtolabs.rundeck.core.execution.workflow;

import com.dtolabs.rundeck.core.rules.Condition;
import com.dtolabs.rundeck.core.rules.MutableStateObj;
import com.dtolabs.rundeck.core.rules.StateObj;

import java.util.Set;

/**
 * Defines how steps in the workflow
 */
public interface WorkflowStrategyProfile {

    /**
     * Get initial state values added to context for this step
     *
     * @param stepNum     step number
     * @param item        workflow
     * @param isFirstStep is first step in the workflow
     *
     * @return initial state
     */
    StateObj getInitialStateForStep(int stepNum, WorkflowExecutionItem item, boolean isFirstStep);

    /**
     * Get the condition set to trigger the step to start
     *
     * @param item        step
     * @param stepNum     step number
     * @param isFirstStep true if it is the first step in the workflow
     *
     * @return start conditions for the step
     */
    Set<Condition> getStartConditionsForStep(
            WorkflowExecutionItem item,
            int stepNum,
            boolean isFirstStep
    );

    /**
     * @param item        step
     * @param stepNum     step number
     * @param isFirstStep true if it is the first step in the workflow
     *
     * @return skip conditions for the step
     */
    Set<Condition> getSkipConditionsForStep(
            WorkflowExecutionItem item,
            int stepNum,
            boolean isFirstStep
    );

    /**
     * Update conditional states for future steps after a step completes.
     * This method is called after each step execution to evaluate conditionals for all
     * subsequent steps that have conditional logic, using the updated SharedDataContext.
     *
     * @param completedStepNum  the step number that just completed
     * @param sharedContext     shared data context containing data from all executed steps
     * @param stateChanges      mutable state object to add conditional evaluation results to
     */
    default void updateConditionalStatesAfterStep(
            int completedStepNum,
            WFSharedContext sharedContext,
            MutableStateObj stateChanges
    ) {
        // Default: no-op (no conditionals to update)
    }

}
