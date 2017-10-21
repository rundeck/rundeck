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

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty;
import com.dtolabs.rundeck.plugins.orchestrator.Orchestrator;
import com.dtolabs.rundeck.plugins.orchestrator.OrchestratorPlugin;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Plugin(name = "subset", service = ServiceNameConstants.Orchestrator)
@PluginDescription(title = "Random Subset", description = "Chooses only a random subset of the target nodes.")
public class RandomSubsetOrchestratorPlugin implements OrchestratorPlugin {


    @PluginProperty(title = "Count", description = "Number of nodes to select from the pool", defaultValue = "1")
    protected int count;

    @Override
    public Orchestrator createOrchestrator(StepExecutionContext context, Collection<INodeEntry> nodes) {
        String ident = createWFLayerIdent(context);
        //use the ident as random seed to allow repeatable sequence of random nodes
        //if this orchestrator config is invoked more than once in the same
        //workflow layer
        return new RandomSubsetOrchestrator(count, context, nodes, (long) ident.hashCode());
    }

    public String createWFLayerIdent(final StepExecutionContext context) {
        //create a string which identifies this execution layer uniquely
        List<Integer> stepContext = context.getStepContext();
        String ident = context.getFrameworkProject();
        if (context.getDataContext().get("job") != null && context.getDataContext().get("job").get("execid") != null) {
            ident += context.getDataContext().get("job").get("execid");
        }

        return ident +
               String.join(",", stepContext.stream().map(Object::toString).collect(Collectors.toList()));
    }
}
