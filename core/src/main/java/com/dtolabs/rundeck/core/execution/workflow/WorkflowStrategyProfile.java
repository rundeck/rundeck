package com.dtolabs.rundeck.core.execution.workflow;

import com.dtolabs.rundeck.core.rules.Condition;
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


}
