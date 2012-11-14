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
* StepPluginAdapter.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 11/13/12 6:30 PM
* 
*/
package com.dtolabs.rundeck.core.execution.workflow.steps;

import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.StepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutor;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResultImpl;
import com.dtolabs.rundeck.core.plugins.configuration.Describable;
import com.dtolabs.rundeck.core.plugins.configuration.Description;
import com.dtolabs.rundeck.plugins.step.PluginStepItem;
import com.dtolabs.rundeck.plugins.step.StepPlugin;

import java.util.*;


/**
 * StepPluginAdapter is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
class StepPluginAdapter implements StepExecutor, Describable {
    private StepPlugin plugin;

    public StepPluginAdapter(final StepPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Description getDescription() {
        if (plugin instanceof Describable) {
            Describable desc = (Describable) plugin;
            return desc.getDescription();
        }
        return null;
    }

    @Override
    public boolean isNodeDispatchStep(StepExecutionItem item) {
        return false;
    }

    @Override
    public StepExecutionResult executeWorkflowStep(final ExecutionContext executionContext, final StepExecutionItem item)
        throws StepException {
        final PluginStepItem step = toPluginStepItem(item);
        final boolean success = plugin.executeStep(executionContext, step);
        return new StepExecutionResultImpl(success);
    }

    private PluginStepItem toPluginStepItem(final StepExecutionItem item) {
        PluginStepItem step;
        if (item instanceof PluginStepItem) {
            step = (PluginStepItem) item;
        } else {
            step = new PluginStepItem() {
                @Override
                public Map<String, Object> getStepConfiguration() {
                    return null;
                }

                @Override
                public String getType() {
                    return item.getType();
                }
            };
        }
        return step;
    }
}
