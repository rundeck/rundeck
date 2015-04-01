
## When Node Executors are invoked

Rundeck executes Command items on Nodes.  The command may be part of a Workflow as defined
in a Job, and it may be executed multiple times on different nodes.

Several "kinds" of Command items can be specified in Workflows:

1. "exec" commands - simple system command strings.
2. "script" commands - either embedded script content, or server-local script.
files can be sent to the specified node and then executed with a set of input arguments.
3. "jobref" commands - references to other Jobs by name that will be executed with
a set of input arguments.
4. custom - Users can use or create their own workflow step plugins.

Rundeck uses the NodeExecutor and FileCopier services as part of the process of 
executing these command types.

The procedure for executing an "exec" command is:

1. load the NodeExecutor provider for the node and context.
2. call the NodeExecutor#executeCommand method.

The procedure for executing a "script" command is:

1. load the FileCopier provider for the node and context.
2. call the FileCopier#copy* method.
3. load the NodeExecutor provider for the node and context.
4. Possibly execute an intermediate command (such as "chmod +x" on the copied file).
5. execute the NodeExecutor#executeCommand method, passing the filepath of the 
  copied file, and any arguments to the script command.

## File Copier destination directory

When executing a Script step, the destination file path to be used when copying the script can be set using Node, Project, or Framework configuration values.

Node Attributes:

* `file-copy-destination-dir`: The directory on the remote node to copy the script file to before executing it. The default value is `C:/WINDOWS/TEMP/` on Windows nodes, and `/tmp` for other nodes.
* `osFamily`: specify "windows" for windows nodes.

The `file-copy-destination-dir` can be set at the project and framework level, and can have a default be specified depending on the osfamily as well:

In `project.properties` file:

* `project.file-copy-destination-dir` - default for all nodes in project
* `project.file-copy-destination-dir.[osfamily]` - default for nodes in the project with specified osfamily

In `framework.properties` file:

* `framework.file-copy-destination-dir` - default for all nodes in framework
* `framework.file-copy-destination-dir.[osfamily]` - default for nodes in the framework with specified osfamily

The order to determine the value is:

1. node-specific
2. project-os-specific
3. project-specific
4. framework-os-specific
5. framework-specific

## Built-in Node Execution plugins

Rundeck includes the following node execution plugins.

`local`

:   local execution of a command.

`jsch-ssh`

:   remote execution of a command via SSH, requiring the "hostname", and "username" attributes on a node.

For FileCopier, these plugins:

`local`

:   creates a local temp file for a script.

`jsch-scp`

:   remote copy of a command via SCP, requiring the "hostname" and  "username" attributes on a node.
