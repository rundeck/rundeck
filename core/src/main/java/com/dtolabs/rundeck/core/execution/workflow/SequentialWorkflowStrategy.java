package com.dtolabs.rundeck.core.execution.workflow;

import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.core.rules.RuleEngine;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;

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
    public void setup(final RuleEngine ruleEngine, StepExecutionContext context, IWorkflow workflow) {

    }

    @Override
    public WorkflowStrategyProfile getProfile() {

        return new SequentialStrategyProfile();
    }

}
