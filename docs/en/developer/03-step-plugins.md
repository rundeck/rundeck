% Workflow Step Plugin
% Greg Schueler
% December 10, 2012

## About 

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

### Use cases

There are several reasons to create a Step Plugin:

* You want to take the set of nodes defined for a Job, and use them with some other batch processing system, such as another kind of remote dispatcher or orchestration tool, rather than executing commands on them directly.
    * You would implement a [WorkflowStep Plugin](#workflowstep-plugin) plugin which is provided with the set of Nodes
* You want to interact with another system on a per-node basis, such as for updating or reporting the state of a Node or process, rather than executing a command on the node
    * You would implement a [WorkflowNodeStep Plugin](#workflownodestep-plugin)
* You want to wrap a command or script in a simplified user interface and have it executed remotely on nodes
    * You would implement a [RemoteScriptNodeStep Plugin](#remotescriptnodestep-plugin) which allows you to define a command or script and declare a custom set of input fields.

## Java Plugin Type

## Define a plugin provider class

Refer to the [Plugin Development - Java Plugins](plugin-development.html#java-plugin-development)
 section for information about correct
definition of a [Plugin](../javadoc/com/dtolabs/rundeck/core/plugins/Plugin.html) class, including packaging as a Jar and annotation.

Be sure to use the `@Plugin` annotation on your provider implementation class
to let it be recognized by Rundeck (See [Plugin Annotations](plugin-annotations.html)). 

Your `service` name should be one of the
three listed below.  The class
[ServiceNameConstants](../javadoc/com/dtolabs/rundeck/plugins/ServiceNameConstants.html) contains static definitions of all Rundeck Service names.

## Workflow Step Types

Your plugins can be one of three types.

* [WorkflowStep](#workflowstep-plugin)
* [WorkflowNodeStep](#workflownodestep-plugin)
* [RemoteScriptNodeStep](#remotescriptnodestep-plugin)

Each plugin type has an associated Java interface.

### Plugin properties

See [Plugin Development - Java Plugins - Descriptions](plugin-development.html#plugin-descriptions)
to learn how to create configuration properties for your plugin using Java annotations.

### WorkflowStep Plugin

Annotate your class with `@Plugin` and use the service name `WorkflowStep`.

Implement the interface [StepPlugin](../javadoc/com/dtolabs/rundeck/plugins/step/StepPlugin.html):

~~~~~~ {.java}
/**
  * Execute the step.
  *
  * @param context       the plugin step context
  * @param configuration Any configuration property values not otherwise applied to the plugin
  *
  * @throws StepException if an error occurs, the failureReason should indicate the reason
  */
public void executeStep(final PluginStepContext context, final Map<String, Object> configuration)
    throws StepException;
~~~~~~~~~

Your implementation should throw a [StepException](../javadoc/com/dtolabs/rundeck/core/execution/workflow/steps/node/NodeStepException.html) if an error occurs.

### WorkflowNodeStep Plugin

Annotate your class with `@Plugin` and use the service name `WorkflowNodeStep`.

Implement the interface [NodeStepPlugin](../javadoc/com/dtolabs/rundeck/plugins/step/NodeStepPlugin.html):

~~~~~ {.java}
/**
 * Execute the plugin step logic for the given node.
 *
 * @param context       the step context
 * @param configuration Any configuration property values not otherwise applied to the plugin
 * @param entry         the Node
 *
 * @throws NodeStepException if an error occurs
 */
public void executeNodeStep(final PluginStepContext context,
                               final Map<String, Object> configuration,
                               final INodeEntry entry)
    throws NodeStepException;
~~~~~~

Your implementation should throw a [StepException](../javadoc/com/dtolabs/rundeck/core/execution/workflow/steps/node/NodeStepException.html) if an error occurs.

### RemoteScriptNodeStep Plugin

These are a specialized use-case of the Node Step
plugin.  They allow you to simply define a command or a script that should be
executed on the remote nodes, and Rundeck will handle the remote execution of the
command/script via the appropriate services.
    
Annotate your class with `@Plugin` and use the service name `RemoteScriptNodeStep`

Implement the interface [RemoteScriptNodeStepPlugin](../javadoc/com/dtolabs/rundeck/plugins/step/RemoteScriptNodeStepPlugin.html):

~~~~ {.java}
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
~~~~~~~

Your implementation should return a [GeneratedScript](../javadoc/com/dtolabs/rundeck/plugins/step/GeneratedScript.html) object.  You can make use of the 
[GeneratedScriptBuilder](../javadoc/com/dtolabs/rundeck/plugins/step/GeneratedScriptBuilder.html) class to generate the appropriate return type using these
two factory methods:

~~~~~~ {.java}
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
~~~~~~~~

### Step context information

Each plugin is passed a [PluginStepContext](../javadoc/com/dtolabs/rundeck/plugins/step/PluginStepContext.html) instance that provides access to
details about the step and its configuration:

~~~~ {.java}
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
~~~~~~

### Example code

See the source directory `examples/example-java-step-plugin` for
examples of all three provider types.

* On github: [example-java-step-plugin](https://github.com/rundeck/rundeck/tree/development/examples/example-java-step-plugin) 

## Script Plugin Type

*Note:* Currently these type of plugins can be implemented as script-based plugins:

* Node Steps - the plugin will execute the script *locally* on the Rundeck server for each node
* Remote Script Node Steps - the plugin will execute the script *remotely* on each node

See the [Script Plugin Development](plugin-development.html#script-plugin-development) 
for the basics of developing script-based plugins for Rundeck.

Use the service name for the plugin type:

* `WorkflowNodeStep`
* `RemoteScriptNodeStep`

For configuration properties, see the [Resource Model Source Plugin - Plugin Properties](resource-model-source-plugin.html#plugin-properties-1).

To define [property scopes](plugin-annotations.html#property-scopes), 
add a `scope` entry in the map for a configuration property:

~~~~ {.yaml}
  config:
    - type: Integer
      name: count
      title: Count
      description: Enter the number of nodes to generate
      scope: Project
~~~~~~~~


