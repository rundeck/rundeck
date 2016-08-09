/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
