package com.dtolabs.rundeck.core.execution.workflow;

import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.core.plugins.configuration.Validator;
import com.dtolabs.rundeck.core.rules.Condition;
import com.dtolabs.rundeck.core.rules.KeyValueEqualsCondition;
import com.dtolabs.rundeck.core.rules.RuleEngine;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;

import java.util.HashSet;
import java.util.Set;

import static com.dtolabs.rundeck.core.execution.workflow.EngineWorkflowExecutor.WORKFLOW_STATE_KEY;
import static com.dtolabs.rundeck.core.execution.workflow.EngineWorkflowExecutor.WORKFLOW_STATE_STARTED;

/**
 * Created by greg on 5/5/16.
 */
@Plugin(name = "parallel", service = ServiceNameConstants.WorkflowStrategy)
@PluginDescription(title = "Parallel", description = "Run all steps in parallel")
public class ParallelWorkflowStrategy implements WorkflowStrategy {
    public static String PROVIDER_NAME = "parallel";

    @Override
    public int getThreadCount() {
        return 0;
    }

    @Override
    public void setup(final RuleEngine ruleEngine, StepExecutionContext context, IWorkflow workflow) {

    }

    @Override
    public Validator.Report validate(final IWorkflow workflow) {

        return null;
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
                conditionHashMap.add(new KeyValueEqualsCondition(WORKFLOW_STATE_KEY, WORKFLOW_STATE_STARTED));
                return conditionHashMap;
            }

        };
    }
}
