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

/*
* PluginNodeStepExecutionItemImpl.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 11/13/12 3:22 PM
* 
*/
package com.dtolabs.rundeck.execution;

import com.dtolabs.rundeck.core.execution.StepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.steps.NodeDispatchStepExecutor;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionItem;
import com.dtolabs.rundeck.core.plugins.PluginConfiguration;

import java.util.*;


/**
 * PluginNodeStepExecutionItemImpl is a Node step execution item
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class PluginNodeStepExecutionItemImpl extends PluginStepExecutionItemImpl implements NodeStepExecutionItem {
    private String nodeStepType;

    public PluginNodeStepExecutionItemImpl(final String type,
                                           final Map stepConfiguration,
                                           final boolean keepgoingOnSuccess,
                                           final StepExecutionItem handler,
                                           final String label,
                                           final List<PluginConfiguration> filterConfigurations,
                                           final boolean enabled
    )
    {
        super(
                NodeDispatchStepExecutor.STEP_EXECUTION_TYPE,
                stepConfiguration,
                keepgoingOnSuccess,
                handler,
                label,
                filterConfigurations,
                enabled
        );
        this.nodeStepType = type;
    }

    public String getNodeStepType() {
        return nodeStepType;
    }

    public void setNodeStepType(final String nodeStepType) {
        this.nodeStepType = nodeStepType;
    }
}
