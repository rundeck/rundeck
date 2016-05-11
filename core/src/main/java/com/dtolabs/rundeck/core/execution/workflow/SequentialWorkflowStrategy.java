package com.dtolabs.rundeck.core.execution.workflow;

import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.core.rules.Condition;
import com.dtolabs.rundeck.core.rules.KeyValueEqualsCondition;
import com.dtolabs.rundeck.core.rules.RuleEngine;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;

import java.util.HashSet;
import java.util.Set;

import static com.dtolabs.rundeck.core.execution.workflow.EngineWorkflowExecutor.STEP_AFTER_KEY;
import static com.dtolabs.rundeck.core.execution.workflow.EngineWorkflowExecutor.VALUE_TRUE;
import static com.dtolabs.rundeck.core.execution.workflow.EngineWorkflowExecutor.stepKey;

/**
 * Created by greg on 5/5/16.
 */
@Plugin(name = "sequential", service = ServiceNameConstants.WorkflowStrategy)
@PluginDescription(title = "Sequential", description = "Run each step in order. Execute a step on all nodes before proceeding to the next step")

public class SequentialWorkflowStrategy implements WorkflowStrategy {
    public static String PROVIDER_NAME = "sequential";

    @Override
    public int getThreadCount() {
        return 1;
    }

    @Override
    public void setup(final RuleEngine ruleEngine) {

    }

    @Override
    public WorkflowStrategyProfile getProfile() {

        return new EngineWorkflowExecutor.BaseProfile() {


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

        };
    }
}
