
RunDeck Plugins
===========

***RunDeck plugin system is currently under development, so this document
is subject to change.***

***[Insert BETA sticker here]***

Plugins for RunDeck contain new Providers for some of the Services used by
the RunDeck core.

RunDeck comes with some built-in providers for these services, but Plugins
let you write your own, or use third-party implementations.

Each Provider defined by a plugin has a unique "name" that is used to identify
it when enabling it.

There are currently two types of Plugins that can be used and developed for
RunDeck:

1. Node Executor Plugins - these provide ways of executing a command on a Node (local or remote)
2. File Copier Plugins - these provide ways of copying files to a Node

Specifics of how providers of these plugins work is listed below.

The current Plugins are "enabled" for particular nodes on a node-specific basis,
or set as a default provider for a project or for the system.

Enabling plugins:

To enable a Node Executor plugin for a node, add an attribute named either:

`node-executor`: specify the provider by name for a non-local node.
OR
`local-node-executor`: specify the provider by name for the local (server) node.

To specify a default remote Node Executor for a project, or framework-wide, specify this property:

`service.NodeExecutor.default.provider`

To specify a default local Node Executor for a project, or framework-wide, specify this property:

`service.NodeExecutor.default.local.provider`

TO enable a FileCopier plugin for a node, add an attribute named either:

`file-copier`: specify the provider by name for a non-local node.
OR
`local-file-copier`: specify the provider by name for the local (server) node.

To specify a default remote File Copier for a project, or framework-wide, specify this property:

`service.FileCopier.default.provider`

To specify a default local File Copier for a project, or framework-wide, specify this property:

`service.FileCopier.default.local.provider`

## When providers are invoked

RunDeck executes Command items on Nodes.  The command may be part of a Workflow as defined
in a Job, and it may be executed multiple times on different nodes.

Currently three "kinds" of Command items can be specified in Workflows:

1. "exec" commands - simple system command strings
2 "script" commands - either embedded script content, or server-local script 
files can be sent to the specified node and then executed with a set of input arguments.
3. "jobref" commands - references to other Jobs by name that will be executed with
a set of input arguments.

RunDeck uses the NodeExecutor and FileCopier services as part of the process of 
executing these command types.

The procedure for executing an "exec" command is:

1. load the NodeExecutor provider for the node and context
2. call the NodeExecutor#executeCommand method

The procedure for executing a "script" command is:

1. load the FileCopier provider for the node and context
2. call the FileCopier#copy* method 
3. load the NodeExecutor provider for the node and context
4. Possibly execute an intermediate command (such as "chmod +x" on the copied file)
5. execute the NodeExecutor#executeCommand method, passing the filepath of the 
  copied file, and any arguments to the script command.

### Lifecycle

Service Provider implementations have a lifecycle that is scoped to the lifetime
of the request/execution context.

Providers are instantiated when needed, and the instance is retained within the
Service for future use.

Provider instances may also be used by multiple threads.

Your provider class should not use any instance fields or un-threadsafe 
operations.

## built-in providers

RunDeck uses a few built-in providers to provide the default service:

For NodeExecutor, these providers:

`local`: local execution of a command 
`jsch-ssh`: remote execution of a command via SSH, requiring the "hostname", and "username" attributes on a node.

For FileCopier, these providers:

`local`: creates a local temp file for a script
`jsch-scp`: remote copy of a command via SCP, requiring the "hostname" and 
   "username" attributes on a node.

Plugin Development
=============

This is a work in progress, and the plugin system is likely to change.

Plugins are currently developed as Java code that is distributed within a Jar 
file and placed in the RunDeck server's classpath before it starts up.

A "Plugin Provider Class" is a java class that implements a particular interface and is
registered as a "Provider" for a particular RunDeck "Service".  

Available Service Names:

* `NodeExecutor` - interface to implement: `com.dtolabs.rundeck.core.execution.service.NodeExecutor`
* `FileCopier` - interface to implement: `com.dtolabs.rundeck.core.execution.service.FileCopier`

Each plugin also defines a "Name" that identifies it for use in RunDeck.  The Name
of a plugin is also referred to as a "Provider Name", as the plugin class is a
provider of a particular service.

You should choose a unique but simple name for your provider.

Each plugin class must have the "com.dtolabs.rundeck.core.plugins.Plugin"
annotation applied to it.

    @Plugin(name="myprovider", service="NodeExecutor")
    public class MyProvider implements NodeExecutor{
    ...

Your provider class must have at least a zero-argument constructor, and optionally 
can have a single-argument constructor with a 
`com.dtolabs.rundeck.core.common.Framework` parameter, in which case your
class will be constructed with this constructor and passed the Framework
instance.

You may log messages to the ExecutionListener available via 
`ExecutionContext#getExecutionListener()` method.

You can also send output to `System.err` and `System.out` and it will be 
captured as output of the execution.

Node Executor Plugins
--------

A Node Executor provider executes a certain command on a remote or 
local node.

Your provider class must implement the `NodeExecutor` interface:

    public interface NodeExecutor {
        public NodeExecutorResult executeCommand(ExecutionContext context, 
            String[] command, INodeEntry node) throws ExecutionException;
    }

File Copier Plugins
---------

A File Copier provider copies a file or script to a remote
or local node.

Your provider class must implement the FileCopier interface:

    public interface FileCopier {
        public String copyFileStream(final ExecutionContext context, InputStream input, INodeEntry node) throws
            FileCopierException;

        public String copyFile(final ExecutionContext context, File file, INodeEntry node) throws FileCopierException;

        public String copyScriptContent(final ExecutionContext context, String script, INodeEntry node) throws
            FileCopierException;
    }


Architecture
------

Installing Java Plugins

Copy the `plugin.jar` to the RunDeck server's
webapp lib dir:

    cp plugin.jar \
        $RDECK_BASE/server/exp/webapp/WEB-INF/lib

2: Modify the framework.properties file, to enable the plugin

Add this line:

    framework.plugins.[ServiceName].classnames=[classname]
