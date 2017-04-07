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

package org.rundeck.plugin.nodes;

import com.dtolabs.rundeck.core.execution.workflow.steps.StepException;
import com.dtolabs.rundeck.core.nodes.ProjectNodeService;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty;
import com.dtolabs.rundeck.plugins.step.PluginStepContext;
import com.dtolabs.rundeck.plugins.step.StepPlugin;

import java.util.Map;


@Plugin(name = NodeRefreshWorkflowStep.PROVIDER_NAME, service = ServiceNameConstants.WorkflowStep)
@PluginDescription(title = "Refresh Project Nodes",
                   description = "Force a refresh of node sources for the project.\n\nThe refreshed nodes will be " +
                                 "available in any subsequent Job Reference step, but not within the current workflow.")


public class NodeRefreshWorkflowStep implements StepPlugin {

    public static final String PROVIDER_NAME = "source-refresh-plugin";

    @PluginProperty(title = "Sleep",
                    description = "Optional sleep time in seconds before refreshing sources.",
                    required = false)
    private Integer sleep;

    @Override
    public void executeStep(PluginStepContext context, Map<String, Object> configuration) throws StepException {
        if (null != sleep && sleep > 0) {
            try {
                Thread.sleep(1000L * sleep);
            } catch (InterruptedException e) {
                context.getLogger().log(2, "InterruptedException: " + e);
                Thread.currentThread().interrupt();
            }
        }
        ProjectNodeService nodeService = context.getExecutionContext().getNodeService();

        nodeService.refreshProjectNodes(context.getFrameworkProject());
        nodeService.getNodeSet(context.getFrameworkProject());
    }


}
