% Workflow Step Plugin Development
% Greg Schueler
% December 10, 2012

## About Rundeck Workflow Execution

When Rundeck executes a Workflow, there are several elements involved
in producing the correct behavior:

* Set of Nodes to execute on
* Number of parallel threads to use
* Workflow execution strategy
* Sequence of workflow steps to execute

Each step in a workflow is either performed by one of the built-in providers,
or by a plugin. Some built-in steps automatically dispatch to multiple nodes
such as command or script execution. Job-Reference steps however *do not*
dispatch to multiple nodes: these steps execute only once in a workflow.

When there are multiple Nodes to execute on, the Node Steps execute multiple
times, although the Workflow Steps execute only once.

If multiple threads are configured for the workflow, then the Node Steps
may be executing in parallel with each other.

Rundeck also uses two different "strategies" to execute the steps in a
workflow:

1. Step-first - execute a step on all Nodes before executing the next step
2. Node-first - execute all steps in sequence for each Node

## Example code

See the [example-java-step-plugin](https://github.com/dtolabs/rundeck/tree/examples/example-java-step-plugin) for
examples of all three plugin types.

## Define a plugin class

Refer to the [Plugin Development - Java Plugin Development](plugin-development.html#java-plugin-development)
 section for information about correct
definition of a Plugin class, including packaging as a Jar and annotation.

You must be sure to use the `@Plugin` annotation on your provider implementation class to
let it be recognized by Rundeck.

Your Service name should be one of the three listed below.

## Workflow Step Plugin types

You can create a plugin to execute a step in a workflow.  

Your plugins can be one of three types. Each plugin type has an associated Java interface, and a predefined
abstract Base implementation with convenient ways to define your plugin's configuration properties.

The class `com.dtolabs.rundeck.plugins.ServiceNameConstants` contains static definitions of all Rundeck Service
names.

1. Workflow step
    * Define a class that extends `com.dtolabs.rundeck.plugins.step.BaseStepPlugin`
    * OR implements `com.dtolabs.rundeck.plugins.step.StepPlugin`
    * Annotate your class with `@Plugin` and use the service name `WorkflowStep`
2. Node step
    * Define a class that extends `com.dtolabs.rundeck.plugins.step.BaseNodeStepPlugin`
    * OR implements `com.dtolabs.rundeck.plugins.step.NodeStepPlugin`
    * Annotate your class with `@Plugin` and use the service name `WorkflowNodeStep`
3. Remote Script Node Step
    * These are a specialized use-case of the Node Step
plugin.  They allow you to simply define a command or a script that should be
executed on the remote nodes, and Rundeck will handle the remote execution of the
command/script via the appropriate services.
    * Define a class that extends `com.dtolabs.rundeck.plugins.step.BaseRemoteScriptNodeStepPlugin`
    * OR implements `com.dtolabs.rundeck.plugins.step.RemoteScriptNodeStepPlugin`
    * Annotate your class with `@Plugin` and use the service name `RemoteScriptNodeStep`

It is recommended that you extend the base implementation to simplify
the interface you have to implement and to use the [Description Annotations](#description-annotations).

### Step context information

Each plugin is passed a `PluginStepContext` instance that provides access to
details about the step and its configuration:

    public interface PluginStepContext {
        /**
         * Return the logger
         */
        public PluginLogger getLogger();
        /**
         * Return the property resolver
         */
        public PropertyResolver getPropertyResolver();
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


### Workflow Step Plugin

1. Extend `com.dtolabs.rundeck.plugins.step.BaseStepPlugin`.

2. Implement the abstract method:

        /**
         * Perform the step and return true if successful
         */
        protected abstract boolean performStep(PluginStepContext context);

Your implementation should return `true` if it was successful, and `false` otherwise.

### Workflow Node Step Plugin

1. Extend `com.dtolabs.rundeck.plugins.step.BaseNodeStepPlugin`

2. Implement the abstract method:

        /**
         * Perform the step for the node and return true if successful
         */
        protected abstract boolean performNodeStep(PluginStepContext context, INodeEntry entry);

Your implementation should return `true` if it was successful, and `false` otherwise.

### Remote Script Node Step Plugin

1. Extend `com.dtolabs.rundeck.plugins.step.BaseRemoteScriptNodeStepPlugin`
2. Implement the abstract method:

        /**
         * Create the GeneratedScript to be executed on the node
         * @see GeneratedScriptBuilder
         */
        public abstract GeneratedScript buildScript(PluginStepContext context, INodeEntry entry);

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

## Plugin Descriptions

To define a plugin that presents custom GUI configuration properties and/or
uses Project/Framework level configuration, you need to provide a Description
of your plugin.  The Description defines metadata about the plugin, such as the
display name and descriptive text to identify it, as well as the list of all
configuration Properties that it supports.

If you are extending the provided plugin Base classes, see below about using
[Description Annotations](#description-annotations) to define your plugin
Description.

If you are implementing the plugin interfaces directly, you will need to do
the following to provide a Description for your plugin:

* Implement the `com.dtolabs.rundeck.core.plugins.configuration.Describable` interface
    * Return a `com.dtolabs.rundeck.core.plugins.configuration.Description` instance
    * You can construct one by using the `com.dtolabs.rundeck.plugins.util.DescriptionBuilder` builder class.

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

Example:

    @Plugin(name="myplugin", service="WorkflowStep")
    @PluginDescription(title="My Plugin", description="Performs a custom step")
    public class MyPlugin extends BaseStepPlugin{
        ...
    }

If you do not add this annotation, the plugin display name will be the same as the provider name, and will have 
no descriptive text when displayed.

### Properties

You can annotate individual fields in your class to define the configuration
properties of your class.  These are the supported Java types for annotated fields:

* String
* Boolean/boolean
* Integer/integer, Long/long

Annotations and their fields:

* `com.dtolabs.rundeck.plugins.descriptions.PluginProperty` - Declares a class field as a plugin configuration property
    * `name` - the property identifier name
    * `title` - the property display name
    * `description` - descriptive text
    * `defaultValue` - default value
    * `required` - (boolean) whether the property is required to have an input value. Default: false.
    * `scope` (PropertyScope) the resolution scope for the property value
* `com.dtolabs.rundeck.plugins.descriptions.SelectValues` - Declares a String property to be a "Select" property, which defines a set of input values that can be chosen from a list
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

## Script-based Step Plugins

Currently you can only implement Node Steps as script-based plugins.

See the [Plugin Development - Script Plugin Development](plugin-development.html#script-plugin-development) 
for the basics of developing script-based plugins for Rundeck.

Use the service name `WorkflowNodeStep`.

For configuration properties, see the [Plugin Development - Configurable Resource Model Source Script Plugin](plugin-development.html#configurable-resource-model-source-script-plugin).

To define property scopes, add a `scope` entry in the map for a configuration property:

          config:
            - type: Integer
              name: count
              title: Count
              description: Enter the number of nodes to generate
              scope: Project

The scope can be one of those defined in the [Property Scopes](#property-scopes) section.
