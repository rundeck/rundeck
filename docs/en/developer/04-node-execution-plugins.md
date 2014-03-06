% Node Executor Plugin
% Greg Schueler, Alex Honor
% November 20, 2010

## About

A Node Executor provider executes a certain command on a remote or 
local node.

## Java Plugin Type

Your provider class must implement the `com.dtolabs.rundeck.core.execution.service.NodeExecutor` interface:

~~~~~ {.java}
public interface NodeExecutor {
     public NodeExecutorResult executeCommand(ExecutionContext context, 
              String[] command, INodeEntry node);
}
~~~~~~~~~

### Plugin properties

A Node Executor can be me made Configurable on a per-project basis via the Web GUI by
implementing the `com.dtolabs.rundeck.core.plugins.configuration.Describable`
interface. It is up to your plugin implementation to use configuration properties
from the `FrameworkProject` instance to configure itself. You must also be sure
to return an appropriate mapping in the `getPropertiesMapping` method of the `Description` interface to declare the property names to be used in the 
`project.properties` file.

See [Plugin Development - Java Plugins - Descriptions](plugin-development.html#plugin-descriptions)
to learn how to create configuration properties for your plugin.

More information is available in the Javadoc.

## Script Plugin Type

See the [Script Plugin Development](plugin-development.html#script-plugin-development) 
for the basics of developing script-based plugins for Rundeck.


### Provider Script equirements

The specific service has expectations about the way your provider script behaves:

Exit code:

* Exit code of 0 indicates success.
* Any other exit code indicates failure.

Output

All output to `STDOUT`/`STDERR` will be captured for the job's output.

