package com.dtolabs.rundeck.core.execution.workflow;

import com.dtolabs.rundeck.core.rules.Condition;
import com.dtolabs.rundeck.core.rules.KeyValueEqualsCondition;

import java.util.HashSet;
import java.util.Set;

import static com.dtolabs.rundeck.core.execution.workflow.EngineWorkflowExecutor.STEP_AFTER_KEY;
import static com.dtolabs.rundeck.core.execution.workflow.EngineWorkflowExecutor.VALUE_TRUE;
import static com.dtolabs.rundeck.core.execution.workflow.EngineWorkflowExecutor.stepKey;

/**
 * profile for sequential step execution
 */
public class SequentialStrategyProfile extends EngineWorkflowExecutor.BaseProfile {


    @Override
    public Set<Condition> getStartConditionsForStep(
            final WorkflowExecutionItem item,
            final int stepNum,
            final boolean isFirstStep
    )
    {
        HashSet<Condition> conditionHashMap = new HashSet<>();
        if (!isFirstStep) {
            conditionHashMap.add(conditionAfterStep(stepNum - 1));
        }
        return conditionHashMap;
    }

    public Condition conditionAfterStep(final int stepNum) {
        return new KeyValueEqualsCondition(stepKey(STEP_AFTER_KEY, stepNum), VALUE_TRUE);
    }

}
