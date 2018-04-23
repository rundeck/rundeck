/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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

package com.dtolabs.rundeck.plugin.stub;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.dispatcher.ContextView;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepException;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepException;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.descriptions.*;
import com.dtolabs.rundeck.plugins.step.NodeStepPlugin;
import com.dtolabs.rundeck.plugins.step.PluginStepContext;

import java.util.Map;
import java.util.Properties;

import static com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants.CODE_SYNTAX_MODE;
import static com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants.DISPLAY_TYPE_KEY;
import static com.dtolabs.rundeck.plugin.stub.StubDataStep.addData;
import static com.dtolabs.rundeck.plugin.stub.StubDataStep.parseData;

/**
 * @author greg
 * @since 5/2/17
 */

@Plugin(service = ServiceNameConstants.WorkflowNodeStep, name = StubDataNodeStep.PROVIDER_NAME)
@PluginDescription(title = "Data Node Step", description = "Produce data values for a node")
public class StubDataNodeStep implements NodeStepPlugin {
    public static final String PROVIDER_NAME = "stub-node-step";

    @PluginProperty(title = "Data",
                    description = "Properties formatted data to set for the current node",
                    required = true)
    @RenderingOptions(
            {
                    @RenderingOption(key = DISPLAY_TYPE_KEY, value = "CODE"),
                    @RenderingOption(key = CODE_SYNTAX_MODE, value = "properties"),
            }
    )
    private String data;

    @PluginProperty(title = "Format",
                    description = "Format for the data",
                    required = true,
                    defaultValue = "properties")
    @SelectValues(values = {"properties", "json", "yaml"})
    private String format;

    @Override
    public void executeNodeStep(
            final PluginStepContext context,
            final Map<String, Object> configuration,
            final INodeEntry entry
    ) throws NodeStepException
    {

        Properties props = null;
        try {
            props = parseData(format, data);
        } catch (StepException e) {
            throw new NodeStepException(e.getMessage(), e, e.getFailureReason(), entry.getNodename());
        }
        addData(
                //instead of global scope, add data at node scope
                ContextView.node(entry.getNodename()),
                context,
                props,
                prop -> prop.replaceAll("@node@", entry.getNodename())
        );
        context.getLogger().log(2, String.format("Added %d data values", props.size()));
    }
}
