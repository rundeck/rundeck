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
* ExampleNodeStepPlugin.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 11/6/12 11:48 AM
* 
*/
package com.dtolabs.rundeck.plugin.example;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.workflow.steps.FailureReason;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepException;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.step.NodeStepPlugin;
import com.dtolabs.rundeck.plugins.step.PluginStepContext;
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder;
import com.dtolabs.rundeck.plugins.util.PropertyBuilder;

import java.util.Map;


/**
 * ExampleNodeStepPlugin demonstrates a basic {@link com.dtolabs.rundeck.plugins.step.NodeStepPlugin}, it extends the
 * {@link com.dtolabs.rundeck.plugins.step.BaseNodeStepPlugin}, and demonstrates how to programmatically build all of
 * the plugin's Properties exposed in the Rundeck GUI.
 * <p/>
 * The plugin class is annotated with {@link Plugin} to define the service and name of this service provider plugin.
 * <p/>
 * The provider name of this plugin statically defined in the class. The service name makes use of {@link
 * ServiceNameConstants} to provide the known Rundeck service names.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
@Plugin(name = ExampleNodeStepPlugin.SERVICE_PROVIDER_NAME, service = ServiceNameConstants.WorkflowNodeStep)
public class ExampleNodeStepPlugin implements NodeStepPlugin, DescriptionBuilder.Collaborator {
    /**
     * Define a name used to identify your plugin. It is a good idea to use a fully qualified package-style name.
     */
    public static final String SERVICE_PROVIDER_NAME = "com.dtolabs.rundeck.plugin.example.ExampleNodeStepPlugin";

    public void buildWith(DescriptionBuilder builder) {
        builder
            .name(SERVICE_PROVIDER_NAME)
            .title("Example Node Step")
            .description("Does nothing")
            .property(PropertyBuilder.builder()
                          .string("monkey")
                          .title("Monkey")
                          .description("Monkey name")
                          .required(true)
                          .build()
            )
            .property(PropertyBuilder.builder()
                          .booleanType("pancake")
                          .title("Panacke")
                          .description("Want Pancake?")
                          .required(false)
                          .defaultValue("false")
                          .build()
            )
            .property(PropertyBuilder.builder()
                          .freeSelect("yogurt")
                          .title("Yogurt")
                          .description("Yogurt type")
                          .required(false)
                          .defaultValue("Vanilla")
                          .values("Vanilla", "Chocolate", "Peach")
                          .build()
            )
            .property(PropertyBuilder.builder()
                          .integer("count")
                          .title("Count")
                          .description("How many?")
                          .required(false)
                          .defaultValue("2")
                          .build()
            )
            .property(PropertyBuilder.builder()
                          .longType("index")
                          .title("Index")
                          .description("How many More?")
                          .required(false)
                          .defaultValue("20")
                          .build()
            )
            .property(PropertyBuilder.builder()
                          .select("icecream")
                          .title("Ice Cream")
                          .description("Ice Cream Flavor")
                          .required(false)
                          .values("Blueberry", "Rambo")
                          .build()
            );
    }
    /**
     * This enum lists the known reasons this plugin might fail
     */
    static enum Reason implements FailureReason{
        PancakeReason
    }

    /**
     * The {@link #performNodeStep(com.dtolabs.rundeck.plugins.step.PluginStepContext,
     * com.dtolabs.rundeck.core.common.INodeEntry)} method is invoked when your plugin should perform its logic for the
     * appropriate node.  The {@link PluginStepContext} provides access to the configuration of the plugin, and details
     * about the step number and context.
     * <p/>
     * The {@link INodeEntry} parameter is the node that should be executed on.  Your plugin should make use of the
     * node's attributes (such has "hostname" or any others required by your plugin) to perform the appropriate action.
     */
    public void executeNodeStep(final PluginStepContext context,
                                final Map<String, Object> configuration,
                                final INodeEntry entry) throws NodeStepException {

        System.out.println("Example node step executing on node: " + entry.getNodename());
        System.out.println("Example step extra config: " + configuration);
        System.out.println("Example step num: " + context.getStepNumber());
        System.out.println("Example step context: " + context.getStepContext());
        if ("true".equals(configuration.get("pancake"))) {
            //throw exception indicating the cause of the error
            throw new NodeStepException("pancake was true", Reason.PancakeReason, entry.getNodename());
        }
    }
}
