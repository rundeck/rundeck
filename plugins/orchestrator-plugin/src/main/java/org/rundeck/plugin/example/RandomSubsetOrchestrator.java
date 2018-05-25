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
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResult;
import com.dtolabs.rundeck.plugins.orchestrator.Orchestrator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

/**
 * Selects a random subset of the nodes
 */
public class RandomSubsetOrchestrator implements Orchestrator {

    Random random;
    long seed;
    final int count;
    List<INodeEntry> nodes;

    public RandomSubsetOrchestrator(
            int count,
            StepExecutionContext context,
            Collection<INodeEntry> nodes, final long seed
    )
    {
        this.seed = seed;
        this.random = new Random(seed);
        this.count = count;
        this.nodes = select(count, nodes);
    }

    /**
     * Select count random items from the input nodes, or if nodes is smaller than count, reorders them
     * @param count number of nodes
     * @param nodes input nodes
     * @return list of count nodes
     */
    private List<INodeEntry> select(final int count, final Collection<INodeEntry> nodes) {
        List<INodeEntry> source = new ArrayList<>(nodes);
        List<INodeEntry> selected = new ArrayList<>();
        int total = Math.min(count, nodes.size());
        for (int i = 0; i < total; i++) {
            selected.add(source.remove(random.nextInt(source.size())));
        }
        return selected;
    }


    @Override
    public INodeEntry nextNode() {
        if (nodes.size() > 0) {
            return nodes.remove(0);
        } else {
            return null;
        }
    }

    @Override
    public void returnNode( final INodeEntry node, final boolean success, final NodeStepResult result)
    {

    }

    @Override
    public boolean isComplete() {
        return nodes.size() == 0;
    }
}
