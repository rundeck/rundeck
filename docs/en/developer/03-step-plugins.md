% Workflow Step Plugin Development
% Greg Schueler
% December 10, 2012

## About Rundeck Workflow Steps

There are two types of steps in a workflow:

1. **Node Steps** -  executed on multiple nodes
    * example: a command or script execution
2. **Workflow Steps** -  only executes once in a workflow
    * example: a Job-Reference step

When there are multiple Nodes to execute on, the Node Steps execute multiple
times, although the Workflow Steps will execute only once. Workflow steps
are always executed in order, so any sequence of steps will be completed
before the next step is executed even if they run on multiple nodes or threads.

You can create a plugin to execute either type of step.

## Use cases

There are several reasons to create a Rundeck Step Plugin:

* You want to take the set of nodes defined for a Job, and use them with some other batch processing system, such as another kind of remote dispatcher or orchestration tool, rather than executing commands on them directly.
    * You would implement a [Workflow Step Plugin](#workflow-step-plugin) plugin which is provided with the set of Nodes
* You want to interact with another system on a per-node basis, such as for updating or reporting the state of a Node or process, rather than executing a command on the node
    * You would implement a [Workflow Node Step Plugin](#workflow-node-step-plugin)
* You want to wrap a command or script in a simplified user interface and have it executed remotely on nodes
    * You would implement a [Remote Script Node Step Plugin](#remote-script-node-step-plugin) which allows you to define a command or script and declare a custom set of input fields.

## Example code

See the source directory `examples/example-java-step-plugin` for
examples of all three provider types.

* On github: [example-java-step-plugin](https://github.com/dtolabs/rundeck/tree/examples/example-java-step-plugin) 

## Define a plugin provider class

Refer to the [Plugin Development - Java Plugin Development](plugin-development.html#java-plugin-development)
 section for information about correct
definition of a Plugin class, including packaging as a Jar and annotation.

Be sure to use the `@Plugin` annotation on your provider implementation class
to let it be recognized by Rundeck. Your `service` name should be one of the
three listed below.  The class
`com.dtolabs.rundeck.plugins.ServiceNameConstants` contains static definitions
of all Rundeck Service names.

## Workflow Step Plugin types

Your plugins can be one of three types. Each plugin type has an associated Java interface.

### Workflow Step Plugin

Annotate your class with `@Plugin` and use the service name `WorkflowStep`.

Implement the interface `com.dtolabs.rundeck.plugins.step.StepPlugin`:

    /**
     * Execute the step, return true if the step succeeded.
     *
     * @param context       the plugin step context
     * @param configuration Any configuration property values not otherwise applied to the plugin
     */
    public boolean executeStep(final PluginStepContext context, final Map<String, Object> configuration)
        throws StepException;

Your implementation should return `true` if it was successful, and `false` otherwise.

### Workflow Node Step Plugin

Annotate your class with `@Plugin` and use the service name `WorkflowNodeStep`.

Implement the interface `com.dtolabs.rundeck.plugins.step.NodeStepPlugin`

    /**
     * Execute the plugin step logic for the given node.
     *
     * @param context       the step context
     * @param configuration Any configuration property values not otherwise applied to the plugin
     * @param entry         the Node
     *
     * @throws NodeStepException if an error occurs
     */
    public boolean executeNodeStep(final PluginStepContext context,
                                   final Map<String, Object> configuration,
                                   final INodeEntry entry)
        throws NodeStepException;

Your implementation should return `true` if it was successful, and `false` otherwise.

### Remote Script Node Step Plugin

These are a specialized use-case of the Node Step
plugin.  They allow you to simply define a command or a script that should be
executed on the remote nodes, and Rundeck will handle the remote execution of the
command/script via the appropriate services.
    
Annotate your class with `@Plugin` and use the service name `RemoteScriptNodeStep`

Implement the interface `com.dtolabs.rundeck.plugins.step.RemoteScriptNodeStepPlugin`

    /**
     * Generate a full script or command string to execute on the remote node
     *
     * @param context       the step context
     * @param configuration Any configuration property values not otherwise applied to the plugin
     * @param entry         the Node
     *
     * @throws NodeStepException if an error occurs
     */
    public GeneratedScript generateScript(final PluginStepContext context,
                                          final Map<String, Object> configuration,
                                          final INodeEntry entry)
        throws NodeStepException;

Your implementation should return a `GeneratedScript` object.  You can make use of the 
`com.dtolabs.rundeck.plugins.step.GeneratedScriptBuilder` class to generate the appropriate return type using these
two factory methods:

    /**
     * Create a script
     *
     * @param script the script text
     * @param args   the arguments for the script
     */
    public static GeneratedScript script(final String script, final String[] args);

    /**
     * Create a command
     *
     * @param command the command and arguments
     */
    public static GeneratedScript command(final String... command);

### Step context information

Each plugin is passed a `PluginStepContext` instance that provides access to
details about the step and its configuration:

    public interface PluginStepContext {
        /**
         * Return the logger
         */
        public PluginLogger getLogger();
        /**
         * Return the project name
         */
        public String getFrameworkProject();
        /**
         * Return the data context
         */
        public Map<String, Map<String, String>> getDataContext();

        /**
         * Return the nodes used for this execution
         */
        public INodeSet getNodes();
        /**
         * Return the step number within the current workflow
         */
        public int getStepNumber();
        /**
         * Return the context path of step numbers within the larger workflow context.
         */
        public List<Integer> getStepContext();
    }

## Plugin Descriptions

To define a plugin that presents custom GUI configuration properties and/or
uses Project/Framework level configuration, you need to provide a Description
of your plugin.  The Description defines metadata about the plugin, such as the
display name and descriptive text, as well as the list of all
configuration Properties that it supports.

There are several ways to declare your plugin's Description:

**Annotations**

The simplest way to declare your plugin description is via
[Description Annotations](#description-annotations) as described in the following section.

**Collaborator interface**

Implement the `com.dtolabs.rundeck.plugins.util.DescriptionBuilder#Collaborator` interface
in your plugin class, and it will be given an opportunity to perform actions on the Builder
object before it finally constructs a Description.

You can use this in addition to the *Annotations* method if you want to modify the static
annotation values.

**Describable interface**

If you want to build the Description object yourself, you can do so by
implementing the `com.dtolabs.rundeck.core.plugins.configuration.Describable`
interface. Return a
`com.dtolabs.rundeck.core.plugins.configuration.Description` instance. You can
construct one by using the
`com.dtolabs.rundeck.plugins.util.DescriptionBuilder` builder class.

This mechanism will bypass the use of Annotations to provide the description.

## Description Annotations

You can use annotations to add metadata about your plugin to the class
definition itself, and Rundeck will extract that metadata for use in
displaying the plugin information and configuration properties in the GUI, as
well as for applying the runtime configuration values to your plugin class
instance when it is being executed.

### Plugin information

You can define the display name, and descriptive text about your plugin by adding a 
`com.dtolabs.rundeck.plugins.descriptions.PluginDescription` annotation to your
plugin class.

Attributes of `@PluginDescription`:

* `title` - the display name for your plugin
* `description` - descriptive text shown next to the display name

Example:

    @Plugin(name="myplugin", service=ServiceNameConstants.WorkflowStep)
    @PluginDescription(title="My Plugin", description="Performs a custom step")
    public class MyPlugin implements StepPlugin{
        ...
    }

*Note:* If you do not add this annotation, the plugin display name will be the same as the provider name, and will have 
no descriptive text when displayed.

### Properties

You can annotate individual fields in your class to define the configuration
properties of your class.  These are the supported Java types for annotated fields:

* String
* Boolean/boolean
* Integer/integer, Long/long

When your plugin is executed, the fields will be set to the appropriate values
based on their default value, scope, and any value set by the user in the
workflow configuration.

These annotation classes are used:

* `com.dtolabs.rundeck.plugins.descriptions.PluginProperty` - Declares a class field as a plugin configuration property
* `com.dtolabs.rundeck.plugins.descriptions.SelectValues` - Declares a String property to be a "Select" property, which defines a set of input values that can be chosen from a list

Attributes:

* `@PluginProperty`
    * `name` - the property identifier name
    * `title` - the property display name
    * `description` - descriptive text
    * `defaultValue` - default value
    * `required` - (boolean) whether the property is required to have an input value. Default: false.
    * `scope` (PropertyScope) the resolution scope for the property value
* `@SelectValues`
    * `values` (String[]) the set of values that can be chosen
    * `freeSelect` (boolean) whether the user can enter values not in the list. Default: false.

Examples:

    @PluginProperty(title = "Name", description = "What is your name?", required = true)
    private String name;
    
    @PluginProperty(title = "Age", description = "How old are you?")
    private int amount;
    
    @PluginProperty(title = "Favorite Fruit",
                    description = "What is your favorite fruit?",
                    defaultValue = "banana")
    @SelectValues(values = {"banana", "lemon", "orange"}, freeSelect = true)
    private String fruit;

#### Property Scopes

You can define the scope for a property by adding `scope` to the PluginProperty annotation.  Refer to the class `com.dtolabs.rundeck.core.plugins.configuration.PropertyScope`.  These are the available scopes and how the property values can be resolved:

* `Framework` - Only framework properties
* `ProjectOnly` - Only Project properties
* `Project` - Project and Framework properties
* `InstanceOnly` - Only instance properties
* `Instance` - Instance and all earlier levels

The default effective scope if you do not specify it in the annotation is `InstanceOnly`.

When resolving a property in a Project or Framework scope, the following properties will be searched:

* Framework scope
    * file: `$RDBASE/etc/framework.properties`
    * property: `framework.plugin.[ServiceName].[providerName].[propertyname]`

* Project scope
    * file: `$RDBASE/projects/[ProjectName]/etc/project.properties`
    * property: `project.plugin.[ServiceName].[providerName].[propertyname]`

## Script-based Step Plugins

*Note:* Currently these type of plugins can be implemented as script-based plugins:

* Node Steps - the plugin will execute the script *locally* on the Rundeck server for each node
* Remote Script Node Steps - the plugin will execute the script *remotely* on each node

See the [Plugin Development - Script Plugin Development](plugin-development.html#script-plugin-development) 
for the basics of developing script-based plugins for Rundeck.

Use the service name for the plugin type:

* `WorkflowNodeStep`
* `RemoteScriptNodeStep`

For configuration properties, see the [Plugin Development - Configurable Resource Model Source Script Plugin](plugin-development.html#configurable-resource-model-source-script-plugin).

To define property scopes, add a `scope` entry in the map for a configuration property:

          config:
            - type: Integer
              name: count
              title: Count
              description: Enter the number of nodes to generate
              scope: Project

The scope can be one of those defined in the [Property Scopes](#property-scopes) section.
