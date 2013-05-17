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
* TestGeneratorPlugin.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 11/20/12 11:15 AM
* 
*/
package com.dtolabs.rundeck.plugin.example;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope;
import com.dtolabs.rundeck.core.plugins.configuration.PropertyValidator;
import com.dtolabs.rundeck.core.plugins.configuration.ValidationException;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty;
import com.dtolabs.rundeck.plugins.descriptions.SelectValues;
import com.dtolabs.rundeck.plugins.descriptions.TextArea;
import com.dtolabs.rundeck.plugins.step.GeneratedScript;
import com.dtolabs.rundeck.plugins.step.GeneratedScriptBuilder;
import com.dtolabs.rundeck.plugins.step.PluginStepContext;
import com.dtolabs.rundeck.plugins.step.RemoteScriptNodeStepPlugin;
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder;
import com.dtolabs.rundeck.plugins.util.PropertyBuilder;

import java.util.Map;


/**
 * ExampleRemoteScriptNodeStepPlugin demonstrates a basic {@link com.dtolabs.rundeck.plugins.step.RemoteScriptNodeStepPlugin},
 * it extends the {@link com.dtolabs.rundeck.plugins.step.BaseRemoteScriptNodeStepPlugin}. This example demonstrates how
 * to annotate instance fields to define the plugin's Properties that should be exposed in the Rundeck GUI. At execution
 * time, they will be automatically set to the appropriate configuration values.  Annotating fields with a specific
 * {@link PropertyScope} can also allow values to be configured in a Rundeck Project or at the global application
 * (Framework) level.
 * <p/>
 * A property "scope" defines how the property value is determined at runtime. When a property value is not found at a
 * particular scope, the search widens to the next scope (with some caveats). Property scopes from narrowest to widest
 * are: <ol> <li>Instance - the value set for a workflow step</li> <li>Project - the value set in the project's
 * configuration properties</li> <li>Framework - the value set in the application configuration properties</li> </ol>
 * Two special scopes "InstanceOnly" and "ProjectOnly" do not allow the search to widen, and must be present in that
 * scope.
 * <p/>
 * The default scope for plugin properties is "InstanceOnly", but you can use any scope for property. Note: only
 * properties of "Instance"/"InstanceOnly" scope will be shown for configuration in the Workflow step GUI.
 * <p/>
 * The plugin class is annotated with {@link Plugin} to define the service and name of this service provider plugin.
 * <p/>
 * The provider name of this plugin statically defined in the class. The service name makes use of {@link
 * ServiceNameConstants} to provide the known Rundeck service names
 * <p/>
 * This plugin is also annotated with {@link PluginDescription} to provide the title (display name) and description text
 * displayed by Rundeck in the GUI.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
@Plugin(service = ServiceNameConstants.RemoteScriptNodeStep,
        name = ExampleRemoteScriptNodeStepPlugin.SERVICE_PROVIDER_NAME)
@PluginDescription(title = "Remote Script Node Step", description = "Generator")
public class ExampleRemoteScriptNodeStepPlugin implements RemoteScriptNodeStepPlugin, DescriptionBuilder.Collaborator {
    /**
     * Define a name used to identify your plugin. It is a good idea to use a fully qualified package-style name.
     */
    public static final String SERVICE_PROVIDER_NAME
        = "com.dtolabs.rundeck.plugin.example.ExampleRemoteScriptNodeStepPlugin";

    /**
     * Defines a string field annotated with {@link PluginProperty} so that it is shown in the Rundeck GUI for this
     * plugin.  The annotation can define a title (display name), description text, default string value, whether it is
     * required, and specific scope for the property.
     *
     * @see com.dtolabs.rundeck.core.plugins.configuration.Property
     */
    @PluginProperty(title = "Funky", description = "Funk name", required = true)
    protected String funky;

    /**
     * Defines a multi-line textarea for a string property.
     */
    @PluginProperty(title = "Thesis", description = "Thesis")
    @TextArea
    protected String thesis;

    /**
     * Defines a boolean field as a property
     */
    @PluginProperty(title = "Jam", description = "Want jam?")
    protected boolean jam;
    /**
     * Defines an integer field as a property, and uses a defaultValue defined as a string
     */
    @PluginProperty(title = "Amount", description = "How amount?", defaultValue = "2")
    protected int amount;
    /**
     * Defines a Long field
     */
    @PluginProperty(title = "Money", description = "how money?", defaultValue = "20")
    protected long money;
    /**
     * This field is annotated to be a "FreeSelect" property.  It is a String field and has a {@link SelectValues}
     * annotation which defines a set of string values that will be shown in a pop-up menu in the GUI.  If "freeSelect"
     * is "true", then the GUI will allow the user to enter any text for the field in addition to selecting a value.  If
     * freeSelect is "false" (the default), then the user will only be able to select values from the pop-up menu.
     */
    @PluginProperty(title = "Fruit",
                    description = "your fruit",
                    defaultValue = "banana",
                    scope = PropertyScope.Instance)
    @SelectValues(values = {"banana", "lemon", "orange"}, freeSelect = true)
    protected String fruit;
    /**
     * This field is a Select property, because it uses the {@link SelectValues} annotation with freeSelect=false.
     */
    @PluginProperty(title = "Cake", description = "Cake flavor")
    @SelectValues(values = {"vanilla", "chocolate"}, freeSelect = false)
    protected String cake;
    /**
     * Here is an example of a field that sets a custom scope of "Project", which indicates that it can be configured in
     * the Project or Framework level configuration settings to supply the value, but will not be shown
     */
    @PluginProperty(title = "Debug", description = "Turn on debug?", scope = PropertyScope.Project)
    protected boolean debug;

    /**
     * This class overrides the method to demonstrate how to modify the description and properties before it is used by
     * Rundeck.  All fields which were annotated as properties can be modified/removed.  In this example, the Plugin
     * description is changed, and the "money" field property is altered to define a custom validator for the field.
     */
    public void buildWith(final DescriptionBuilder builder) {
        //override the annotated description of this plugin
        builder.title("Example Remote Script Node Step");
        builder.description("Demonstrates a remote script node step");

        /**
         * calling DescriptionBuilder.property() will replace an existing property of
         * the same name.
         */
        builder.property(
            /**
             * In this case builder.property("money") returns the existing property we
             * have already defined via annotation, allowing us to modify it with the {@link PropertyBuilder}
             *
             */
            builder.property("money")
                /**
                 * change the property's description
                 */
                .description("How much money? (1..5)")
                    /**
                     * set a custom validator for the property
                     */
                .validator(new PropertyValidator() {
                    public boolean isValid(String s) throws ValidationException {
                        try {
                            final int i = Integer.parseInt(s);
                            if (i <= 0 || i >= 6) {
                                //throw ValidationException to indicate a problem, with a reason
                                throw new ValidationException("Must be between 1 and 5");
                            }
                        } catch (NumberFormatException e) {
                            throw new ValidationException("Not a valid integer");
                        }
                        return true;
                    }

                })
        );

        /**
         * Here we create a wholly new property not bound to an existing instance field.  The runtime
         * value for this property will be included in the input configuration map when the plugin method is called.
         */
        builder.property(
            PropertyBuilder.builder()
                .string("fakey")
                .title("Fake")
                .description("Extra value")
        );
    }


    /**
     * Here your plugin should create a script or command to execute on the given remote node.
     * <p/>
     * The {@link GeneratedScriptBuilder} provides a factory for returning the correct type.
     */
    public GeneratedScript generateScript(final PluginStepContext context,
                                          final Map<String, Object> configuration,
                                          final INodeEntry entry) {
        if (debug) {
            System.err.println("DEBUG for ExampleRemoteScriptNodeStepPlugin is true");
        }
        if (jam) {
            /**
             * Returns a script to execute
             */
            return GeneratedScriptBuilder.script(
                    "#!/bin/bash\n"
                            + "echo this is node " + entry.getNodename() + "\n"
                            + "echo stepnum " + context.getStepNumber() + "\n"
                            + "echo step context " + context.getStepContext() + "\n"
                            + "echo funky is " + funky + "\n"
                            + "echo fruit is " + fruit + "\n"
                            + "echo amount is " + amount + "\n"
                            + "echo money is " + money + "\n"
                            + "echo cake is " + cake + "\n"
                            + "echo extra: " + configuration + "\n"
                            + "echo thesis: '" + thesis.replaceAll("'", "'\"'\"'") + "'\n"
                    ,
                    null

            );
        } else {
            /**
             * Returns a command to execute.
             */
            return GeneratedScriptBuilder.command("echo",
                                                  context.getStepNumber() + " " + context.getStepContext() + " " +
                                                  "Hi funky is" +
                                                  " (" + funky + ")" +
                                                  " jam is" + jam
            );
        }
    }

}
