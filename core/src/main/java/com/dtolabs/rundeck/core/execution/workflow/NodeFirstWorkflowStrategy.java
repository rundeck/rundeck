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
* Author: Greg Schueler <a href="mailto:greg@simplifyops.com">greg@simplifyops.com</a>
*/
package com.dtolabs.rundeck.core.execution.workflow;

import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.core.plugins.configuration.Validator;
import com.dtolabs.rundeck.core.rules.RuleEngine;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty;
import com.dtolabs.rundeck.plugins.descriptions.RenderingOption;
import com.dtolabs.rundeck.plugins.descriptions.RenderingOptions;

import static com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants.*;
import static com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants.DisplayType.STATIC_TEXT;

/**
 * Created by greg on 5/11/16.
 */
@Plugin(name = "node-first", service = ServiceNameConstants.WorkflowStrategy)
@PluginDescription(title = "Node First",
                   description = "Execute all steps on a node before proceeding to the next node.")

public class NodeFirstWorkflowStrategy implements WorkflowStrategy {
    public static String PROVIDER_NAME = "node-first";
    @PluginProperty(
            title = " ",
            defaultValue = "<table>\n" +
                           "    <tr><td>1.</td><td class=\"text-info\">NodeA</td> <td>step 1</td></tr>\n" +
                           "    <tr><td>2.</td><td class=\"text-info\">\"</td> <td>step 2</td></tr>\n" +
                           "    <tr><td>3.</td><td class=\"text-info\">\"</td> <td>step 3</td></tr>\n" +
                           "    <tr><td>4.</td><td class=\"text-muted\">NodeB</td> <td>step 1</td></tr>\n" +
                           "    <tr><td>5.</td><td class=\"text-muted\">\"</td> <td>step 2</td></tr>\n" +
                           "    <tr><td>6.</td><td class=\"text-muted\">\"</td> <td>step 3</td></tr>\n" +
                           "</table>"
    )
    @RenderingOptions(
            {
                    @RenderingOption(key = DISPLAY_TYPE_KEY, value = "STATIC_TEXT"),
                    @RenderingOption(key = STATIC_TEXT_CONTENT_TYPE_KEY, value = "text/html"),
                    @RenderingOption(key = GROUP_NAME, value = "Explain"),
                    @RenderingOption(key = GROUPING, value = "secondary"),
            }
    )
    String info;


    @Override
    public int getThreadCount() {
        return 1;
    }

    @Override
    public void setup(final RuleEngine ruleEngine, StepExecutionContext context, IWorkflow workflow) {

    }

    @Override
    public Validator.Report validate(final IWorkflow workflow) {

        return null;
    }

    @Override
    public WorkflowStrategyProfile getProfile() {
        return new SequentialStrategyProfile();
    }
}
