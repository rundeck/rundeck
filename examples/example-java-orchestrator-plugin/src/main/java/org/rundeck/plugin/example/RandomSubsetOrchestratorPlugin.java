package org.rundeck.plugin.example;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty;
import com.dtolabs.rundeck.plugins.orchestrator.Orchestrator;
import com.dtolabs.rundeck.plugins.orchestrator.OrchestratorPlugin;

import java.util.Collection;

@Plugin(name = "subset", service = ServiceNameConstants.Orchestrator)
@PluginDescription(title = "Random Subset", description = "Chooses only a random subset of the target nodes.")
public class RandomSubsetOrchestratorPlugin implements OrchestratorPlugin {


    @PluginProperty(title = "Count", description = "Number of nodes to select from the pool", defaultValue = "1")
    protected int count;

    @Override
    public Orchestrator createOrchestrator(StepExecutionContext context, Collection<INodeEntry> nodes) {
        return new RandomSubsetOrchestrator(count, context, nodes);
    }
}
