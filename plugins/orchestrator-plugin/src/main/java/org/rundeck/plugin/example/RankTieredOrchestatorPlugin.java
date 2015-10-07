package org.rundeck.plugin.example;

import java.util.Collection;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;
import com.dtolabs.rundeck.plugins.orchestrator.Orchestrator;
import com.dtolabs.rundeck.plugins.orchestrator.OrchestratorPlugin;

@Plugin(name = "rankTiered", service = ServiceNameConstants.Orchestrator)
@PluginDescription(title = "Rank Tiered", description = "Process nodes tiered by rank.\n\n" +
                                                        "Will never process the next rank until all nodes in the " +
                                                        "previous rank are complete. " +
                                                        "Uses the configured Rank ordering for the Job.\n\n" +
                                                        "**Note**: if a *Rank Attribute* is not set, then the node " +
                                                        "names are used as the rank attribute, and the behavior of " +
                                                        "this orchestrator will essentially be single threaded.")
public class RankTieredOrchestatorPlugin implements OrchestratorPlugin {
    @Override
    public Orchestrator createOrchestrator(StepExecutionContext context, Collection<INodeEntry> nodes) {
        return new RankTieredOrchestator(context, nodes);
    }

}
