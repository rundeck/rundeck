/*
 * Copyright 2012 DTO Labs, Inc. (http://dtolabs.com)
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
 *
 */

/*
* NodeStepPluginAdapter.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 11/12/12 5:39 PM
* 
*/
package com.dtolabs.rundeck.core.execution.workflow.steps.node;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.StepExecutionItem;
import com.dtolabs.rundeck.core.plugins.configuration.Describable;
import com.dtolabs.rundeck.core.plugins.configuration.Description;
import com.dtolabs.rundeck.plugins.step.NodeStepPlugin;
import com.dtolabs.rundeck.plugins.step.PluginStepItem;

import java.util.*;


/**
 * NodeStepPluginAdapter is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
class NodeStepPluginAdapter implements NodeStepExecutor, Describable {
    @Override
    public Description getDescription() {
        if (plugin instanceof Describable) {
            Describable desc = (Describable) plugin;
            return desc.getDescription();
        }
        return null;
    }

    private NodeStepPlugin plugin;

    public NodeStepPluginAdapter(final NodeStepPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public NodeStepResult executeNodeStep(ExecutionContext context, final NodeStepExecutionItem item, INodeEntry node)
        throws NodeStepException {
        PluginStepItem step;
        if (item instanceof PluginStepItem) {
            step = (PluginStepItem) item;
            //TODO: replace data references in configuration
        } else {
            step = new PluginStepItem() {
                @Override
                public Map<String, Object> getStepConfiguration() {
                    return null;
                }

                @Override
                public String getType() {
                    return item.getNodeStepType();
                }
            };
        }
        boolean success = plugin.executeNodeStep(context, step, node);
        return new NodeStepResultImpl(success, node);
    }
}
