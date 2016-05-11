package com.dtolabs.rundeck.core.execution.workflow;

import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.core.rules.RuleEngine;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;

/**
 * Created by greg on 5/11/16.
 */
@Plugin(name = "node-first", service = ServiceNameConstants.WorkflowStrategy)
@PluginDescription(title = "Node First", description = "Execute all steps on a node before proceeding to the next node.")

public class NodeFirstWorkflowStrategy implements WorkflowStrategy {
    public static String PROVIDER_NAME = "node-first";
    @Override
    public int getThreadCount() {
        return 1;
    }

    @Override
    public void setup(final RuleEngine ruleEngine) {

    }

    @Override
    public WorkflowStrategyProfile getProfile() {
        return new SequentialStrategyProfile();
    }
}
