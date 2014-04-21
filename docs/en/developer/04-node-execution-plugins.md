% Node Executor Plugin
% Greg Schueler, Alex Honor
% November 20, 2010

## About

A Node Executor provider executes a certain command on a remote or 
local node.

## Java Plugin Type

Your provider class must implement the interface
[NodeExecutor](../javadoc/com/dtolabs/rundeck/core/execution/service/NodeExecutor.html):

~~~~~ {.java}
public interface NodeExecutor {
     public NodeExecutorResult executeCommand(ExecutionContext context, 
              String[] command, INodeEntry node);
}
~~~~~~~~~

### Plugin properties

See [Plugin Development - Java Plugins - Descriptions](plugin-development.html#plugin-descriptions)
to learn how to create configuration properties for your plugin.


## Script Plugin Type

See the [Script Plugin Development](plugin-development.html#script-plugin-development) 
for the basics of developing script-based plugins for Rundeck.

### Additional data context properties

The data context used in the script plugin definition can use these additional properties:

`${exec.command}`

  : The user command to execute.

For example, in the `metadata.yaml`, you could pass the command string to your script implementation as:

    script-args: ${node.name} ${exec.command}

### Provider Script requirements

The specific service has expectations about the way your provider script behaves:

#### Script Exit Code

* Exit code of 0 indicates success.
* Any other exit code indicates failure.

#### Script Output

All output to `STDOUT`/`STDERR` will be captured for the job's output.

