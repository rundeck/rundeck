package com.dtolabs.rundeck.core.execution.workflow;

import com.dtolabs.rundeck.core.execution.StepExecutionItem;
import com.dtolabs.rundeck.core.rules.Condition;
import com.dtolabs.rundeck.core.rules.RuleEngine;
import com.dtolabs.rundeck.core.rules.StateObj;

import java.util.Set;

/**
 * Created by greg on 5/3/16.
 */
public interface WorkflowStrategyProfile {

    /**
     * @param stepNum
     * @param item
     * @param isFirstStep
     *
     * @return initial state
     */
    StateObj getInitialStateForStep(int stepNum, WorkflowExecutionItem item, boolean isFirstStep);

    /**
     * @param strategy    strategy
     * @param item        step
     * @param stepNum     step number
     * @param isFirstStep true if it is the first step in the workflow
     *
     * @return start conditions for the step
     */
    Set<Condition> getStartConditionsForStep(
            String strategy,
            WorkflowExecutionItem item,
            int stepNum,
            boolean isFirstStep
    );


}
