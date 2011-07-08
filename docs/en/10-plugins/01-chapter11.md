
RunDeck Plugins
===========

***RunDeck plugin system is currently under development, so this document
is subject to change.***

***[Insert BETA sticker here]***

Plugins for RunDeck contain new Providers for some of the Services used by
the RunDeck core.

RunDeck comes with some built-in providers for these services, but Plugins
let you write your own, or use third-party implementations.

RunDeck currently comes installed with a few useful plugins: script-plugin and 
stub-plugin.  See [Pre-installed plugins](#pre-installed-plugins) for more info.

## Installing Plugins

Installation of plugins is simple:

Put the plugin file, such as `plugin.jar` or `some-plugin.zip`, into the RunDeck 
server's libext dir:

    cp some-plugin.zip $RDECK_BASE/libext

The plugin is now enabled, and any providers it defines can be used by nodes
or projects.

The RunDeck server does not have to be restarted.

## Uninstalling or Updating Plugins

You can simply remove the plugin files from `$RDECK_BASE/libext` to uninstall
them.

You can overwrite an old plugin with a newer version to update it.

## About Services and Providers

The RunDeck core makes use of several different "Services" that provide
functionality for the different steps necessary to execute workflows, jobs, 
and commands across multiple nodes.

Each Service makes use of "Providers". Each Provider has a unique "Provider Name"
that is used to identify it, and most Services have default Providers unless
you specify different ones to use.

![RunDeck Services and Providers](figures/fig1101.png)

There are currently two types of Providers that can be used and developed for
RunDeck:

1. Node Executor Providers - these define ways of executing a command on a Node (local or remote)
2. File Copier Providers - these define ways of copying files to a Node

Specifics of how providers of these plugins work is listed below.

RunDeck Plugins can contain more than one Provider.

## Using Providers

The Providers are "enabled" for particular nodes on a node-specific basis,
or set as a default provider for a project or for the system.

If multiple providers are defined the most specific definition takes precedence
in this order:

1. Node specific
2. Project scope
3. Framework scope

### Node Specific

To enable a provider for a node, add an attribute to the node definition.

Node Executor provider attributes:

`node-executor`

:    specifies the provider name for a non-local node.

`local-node-executor`

:    specifies the provider name for the local (server) node.


FileCopier provider attributes:

`file-copier`

:    specifies the provider by name for a non-local node.

`local-file-copier`

:    specifies the provider by name for the local (server) node.

Example Node in YAML specifying `stub` NodeExecutor and FileCopier:

    remotehost:
        hostname: remotehost
        node-executor: stub
        file-copier: stub

### Project or Framework Scope

You can define the default provider to use for nodes at either the Project or
Framework scope (or both).  To do so, configure any of the following properties
in the `project.properties` or the `framework.properties` files.  

`service.NodeExecutor.default.provider`

:   Specifies the default NodeExecutor provider for remote nodes

`service.NodeExecutor.default.local.provider`

:   Specifies the default Node Executor provider for the  local node.

`service.FileCopier.default.provider`

:   Specifies the default File Copier provider for remote nodes.

`service.FileCopier.default.local.provider`

:   Specifies the default File Copier provider for the local node.

Example `project.properties` to set default local providers to `stub`:

    service.NodeExecutor.default.local.provider=stub
    service.FileCopier.default.local.provider=stub

## When providers are invoked

RunDeck executes Command items on Nodes.  The command may be part of a Workflow as defined
in a Job, and it may be executed multiple times on different nodes.

Currently three "kinds" of Command items can be specified in Workflows:

1. "exec" commands - simple system command strings
2. "script" commands - either embedded script content, or server-local script 
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

## Built-in providers

RunDeck uses a few built-in providers to provide the default service:

For NodeExecutor, these providers:

`local`

:   local execution of a command 

`jsch-ssh`

:   remote execution of a command via SSH, requiring the "hostname", and "username" attributes on a node.

For FileCopier, these providers:

`local`

:   creates a local temp file for a script

`jsch-scp`

:   remote copy of a command via SCP, requiring the "hostname" and  "username" attributes on a node.

## Pre-installed plugins

RunDeck comes with two pre-installed plugins that may be useful, and also serve
as examples of plugin development and usage.

### script-plugin

The `script-plugin` includes these providers:

* `script-exec` for the NodeExecutor service
* `script-copy` for the FileCopier service

(Refer to [Using Providers](#using-providers) to enable them.)

This plugin provides the ability to specify an external script or command
to perform a remote or local execution of a Rundeck command, and remote or local file copies.

It can be a replacement for the built-in SSH-based remote execution and SCP-based file-copy mechanism to
allow you to user whatever external mechanism you wish.

Note: this plugin offers similar functionality to the 
[Script Plugin Development](#script-plugin-development) 
 model.  You may want to use this plugin to test your scripts, and
then later package them into a standalone plugin using that model.  

#### Configuring script-exec

To configure the plugin you must specify a commandline string to execute.  Optionally
you may specify a directory to be used as the working directory when executing
the commandline string.

You can configure these across all projects (framework-wide), a single project 
(project-wide), or specifically for each node, with the most specific configuration
value taking precedence.

#### Configuring the command for script-exec

For Framework and Project-wide, configure a property in either the framework.properties or 
project.properties files:

`plugin.script-exec.default.command`

:   Specifies the default system command to run

For node-specific add an attribute named `script-exec` to the node.

`script-exec`

:   Specifies the system command to run

See [Defining the script-exec command](#defining-the-script-exec-command) for
what to specify for this property.

#### Configuring the working directory

For Framework and Project-wide, configure a property in either the framework.properties or 
project.properties files:

`plugin.script-exec.default.dir`

:   Specifies the default working directory for the execution

For node-specific add an attribute named `script-exec-dir` to the node.

`script-exec-dir`

:   Specifies the default working directory for the execution (optional)

#### Defining the script-exec command

The value of this property or attribute should be the complete commandline 
string to execute in an external system process.

You can use *Data context properties* as you can in normal Rundeck command 
execution, such as `${node.name}` or `${job.name}`. 

In addition, the plugin provides these new data context properties:

`exec.command`

:   The command that the workflow/user has specified to run on the node

`exec.dir`

:   The working directory path if it is configured for the node or in a properties file

Example:

If you wanted to run some external remote connection command ("/bin/execremote") in lieu of the 
built-in ssh command, you could specify these attributes for node:

    mynode:
        node-executor: script-exec
        script-exec: /bin/execremote -host ${node.hostname} -user ${node.username} -- ${exec.command}

At run time, the properties specified would be expanded to the values for the
specific node and command string to execute.

OR, you could specify a default to apply to all nodes within the project.properties 
file located at `$RDECK_BASE/projects/NAME/etc/project.properties`.

    script-exec.default.command= /bin/execremote -host ${node.hostname} \
        -user ${node.username} -- ${exec.command}

Similarly for the `$RDECK_BASE/etc/framework.properties` file to apply to all
projects.

#### Requirements for the script-exec command

The command run by by the script plugin is expected to behave in the following manner:

* Exit with a system exit code of "0" in case of success.
* Any other exit code indicates failure

Note: all output from STDOUT and STDERR will be captured as part of the Rundeck job execution.

#### Configuring script-copy

To configure script-copy you must specify a commandline string to execute.  Optionally
you may specify a directory to be used as the working directory when executing
the commandline string.

You can configure these across all projects (framework-wide), a single project
(project-wide), or specifically for each node, with the most specific configuration
value taking precedence.

#### Configuring the command for script-copy

For Framework and Project-wide, configure a property in either the framework.properties or
project.properties files:

`plugin.script-copy.default.command`

:   Specifies the default system command to run

For node-specific add an attribute named `script-copy` to the node.

`script-copy`

:   Specifies the system command to run

See [Defining the script-copy command](#defining-the-script-copy-command) for
what to specify for this property.

#### Configuring the working directory

For Framework and Project-wide, configure a property in either the framework.properties or
project.properties files:

`plugin.script-copy.default.dir`

:   Specifies the default working directory for the execution


For node-specific add an attribute named `script-copy-dir` to the node.

`script-copy-dir`

:   Specifies the default working directory for the execution (optional)


#### Defining the script-copy command

The value of this property or attribute should be the complete commandline
string to execute in an external system process.

You can use *Data context properties* as you can in normal Rundeck command
execution, such as `${node.name}` or `${job.name}`.

In addition, the plugin provides these new data context properties:

`file-copy.file`

:   The local filepath that should be copied to the remote node


Example:

If you wanted to run some external remote connection command ("/bin/execremote") in lieu of the
built-in ssh command, you could specify these attributes for node:

    mynode:
        node-executor: script-exec
        script-exec: /bin/execremote -host ${node.hostname} -user ${node.username} -- ${exec.command}

At run time, the properties specified would be expanded to the values for the
specific node and command string to execute.

OR, you could specify a default to apply to all nodes within the project.properties
file located at `$RDECK_BASE/projects/NAME/etc/project.properties`.

    script-exec.default.command= /bin/execremote -host ${node.hostname} \
        -user ${node.username} -- ${exec.command}

Similarly for the `$RDECK_BASE/etc/framework.properties` file to apply to all
projects.

#### Requirements of script-copy command

The command executed by script-copy is expected to behave in the following manner:

* Output the filepath of the copied file on the target node as the first line of output on STDOUT
* Exit with an exit code of "0" to indicate success
* Exit with any other exit code indicates failure

#### Example Scripts

Here are some example scripts to show the some possible usage patterns.

**Example script-exec**:

Node definition:

    mynode:
        node-executor: script-exec

Project config `project.properties` file:

    plugin.script-exec.default.command: /tmp/myexec.sh ${node.hostname} ${node.username} -- ${exec.command}

Contents of `/tmp/myexec.sh`:

    #!/bin/bash

    # args are [hostname] [username] -- [command to exec...]

    host=$1
    shift
    user=$1
    shift
    command="$*"

    REMOTECMD=ssh

    exec $REMOTECMD $user@$host $command

**Example script-copy**:

Node definition:

    mynode:
        file-copier: script-copy
        destdir: /some/node/dir

System-wide config in `framework.properties`:

    plugin.script-copy.default.command: /tmp/mycopy.sh ${node.hostname} ${node.username} ${node.destdir} ${file-copy.file}

Contents of `/tmp/mycopy.sh`:

    #!/bin/bash

    # args are [hostname] [username] [destdir] [filepath]

    host=$1
    shift
    user=$1
    shift
    dir=$1
    shift
    file=$1

    name=`basename $file`

    # copy to node
    CPCMD=scp

    exec $CPCMD $file $user@$host:$dir/$name > /dev/null || exit $?

    echo "$dir/$name"

### stub-plugin

The `stub-plugin` includes these providers:

* `stub` for the NodeExecutor service
* `stub` for the FileCopier service

(Refer to [Using Providers](#using-providers) to enable them.)

This plugin does not actually perform any remote file copy or command execution,
instead it simply echoes the command that was supposed to be executed, and
pretends to have copied a file. 

This is intended for use in testing new Nodes, Jobs or Workflow sequences without
affecting any actual runtime environment.  

You can also test some failure scenarios by configuring the following node attributes:

`stub-exec-success`="true/false"

:   If set to false, the stub command execution will simulate command failure

`stub-result-code`

:   Simulate the return result code from execution

You could, for example, disable or test an entire project's workflows or jobs by
simply setting the `project.properties` node executor provider to `stub`.

Plugin Development
=============

This is a work in progress, and the plugin system is likely to change.

There are currently two ways to develop plugins:

1. Develop Java code that is distributed within a Jar 
file.  See [Java plugin development](#java-plugin-development).
2. Write shell/system scripts that implement your desired behavior and put them
in a zip file with some metadata.   See [Script Plugin Development](#script-plugin-development).

Either way, the resultant plugin archive file, either a .jar java archive, 
or a .zip file archive, will be placed in the `$RDECK_BASE/libext` dir.

Java Plugin Development
--------

Java plugins are distributed as .jar files containing the necessary classes for 
one or more service provider, as well as any other java jar dependency files.

The `.jar` file you distribute must have this metadata within the main Manifest
for the jar file to be correctly loaded by the system:

* `Rundeck-Plugin-Version: 1.0`
* `Rundeck-Plugin-Archive: true`
* `Rundeck-Plugin-Classnames: classname,..`
* `Rundeck-Plugin-Libs: lib/something.jar ...` *(optional)*

Each classname listed must be a valid "Provider Class" as defined below.

### Provider Classes

A "Provider Class" is a java class that implements a particular interface and declares
itself as a provider for a particular RunDeck "Service".  

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

### Jar Dependencies

If your Java classes require external libraries that are not included with
the RunDeck runtime, you can include them in your .jar archive. (Look in
`$RDECK_BASE/tools/lib` to see the set of
third-party jars that are available for your classes by default at runtime).

Specify the `Rundeck-Plugin-Libs` attribute in the Main attributes of the
Manifest for the jar, set the value to a space-separated list of jar file names
as you have included them in the jar.

E.g.:

    Rundeck-Plugin-Libs: lib/somejar-1.2.jar lib/anotherjar-1.3.jar

Then include the jar files in the Plugin's jar contents:

    META-INF/
    META-INF/MANIFEST.MF
    com/
    com/mycompany/
    com/mycompany/rundeck/
    com/mycompany/rundeck/plugin/
    com/mycompany/rundeck/plugin/test/
    com/mycompany/rundeck/plugin/test/TestNodeExecutor.class
    lib/
    lib/somejar-1.2.jar
    lib/anotherjar-1.3.jar

### Available Services:

* `NodeExecutor` - executes a command on a node
* `FileCopier` - copies a file to a node

## Provider Lifecycle

Provider classes are instantiated when needed by the Framework object, and the
instance is retained within the Service for future reuse. The Framework
object may exist across multiple executions, and the provider instance may be
reused.

Provider instances may also be used by multiple threads.

Your provider class should not use any instance fields and should be
careful not to use un-threadsafe operations.

### Node Executor Providers

A Node Executor provider executes a certain command on a remote or 
local node.

Your provider class must implement the `com.dtolabs.rundeck.core.execution.service.NodeExecutor` interface:

    public interface NodeExecutor {
        public NodeExecutorResult executeCommand(ExecutionContext context, 
            String[] command, INodeEntry node) throws ExecutionException;
    }

More information is available in the Javadoc.

### File Copier Providers

A File Copier provider copies a file or script to a remote
or local node.

Your provider class must implement the `com.dtolabs.rundeck.core.execution.service.FileCopier` interface:

    public interface FileCopier {
        public String copyFileStream(final ExecutionContext context, InputStream input, INodeEntry node) throws
            FileCopierException;

        public String copyFile(final ExecutionContext context, File file, INodeEntry node) throws FileCopierException;

        public String copyScriptContent(final ExecutionContext context, String script, INodeEntry node) throws
            FileCopierException;
    }

More information is available in the Javadoc.

Script Plugin Development
-----------

Script plugins can provide the same services as Java plugins, but they do so
with a script that is invoked in an external system processes by the JVM. 

You must create a zip file with the following structure:

    [name]-plugin.zip
    \- [name]-plugin/ -- root directory of zip contents, same name as zip file
       |- plugin.yaml -- plugin metadata file
       \- contents/
          |- ...      -- script or resource files
          \- ...

Here is an example:

    $ unzip -l example-1.0-plugin.zip 
    Archive:  example-1.0-plugin.zip
      Length     Date   Time    Name
     --------    ----   ----    ----
            0  04-12-11 11:31   example-1.0-plugin/
            0  04-11-11 15:31   example-1.0-plugin/contents/
         2142  04-11-11 15:31   example-1.0-plugin/contents/script1.sh
         1591  04-11-11 13:10   example-1.0-plugin/contents/script2.sh
          576  04-12-11 10:58   example-1.0-plugin/plugin.yaml
     --------                   -------
         4309                   5 files

The filename of the plugin zip must end with "-plugin.zip" to be recognized as a
plugin archive. The zip must contain a top-level directory with the same base name 
as the zip file (sans ".zip").

The file `plugin.yaml` must have this structure:

    # yaml plugin metadata
    
    name: plugin name
    version: plugin version
    rundeckPluginVersion: 1.0
    author: author name
    date: release date
    providers:
        - name: provider
          service: service name
          plugin-type: script
          script-interpreter: [interpreter]
          script-file: [script file name]
          script-args: [script file args]

The main metadata that is required:

* `name` - name for the plugin
* `version` - version number of the plugin
* `rundeckPluginVersion` - Rundeck Plugin type version, currently "1.0"
* `providers` - list of provider metadata maps

These are optional:

* `author` - optional author info
* `date` - optional release date info

This provides the necessary metadata about the plugin, including one or more 
entries in the `providers` list to declare those providers defined in the plugin.

### Provider metadata

Required provider entries:

* `name` - provider name
* `service` - service name, one of these valid services:
    * `NodeExecutor`
    * `FileCopier`
* `plugin-type` - must be "script" currently.
* `script-file` - must be the name of a file relative to the `contents` directory

Optional entries:

* `script-interpreter` - A system command that should be used to execute the 
    script.  This can be a single binary path, e.g. `/bin/bash`, or include
    any args to the command, such as `/bin/bash -c`.
* `script-args` - the arguments to use when executing the script file.

### How script plugin providers are invoked

When the provider is used for node execution or file copying, the script file,
interpreter, and args are combined into a commandline executed by the system in
this pattern:

    [interpreter] [filename] [args...]

If the interpreter is not specified, then the script file is executed directly,
and that means it must be acceptable by the system to be executed directly (include
any necessary `#!` line, etc).

`script-args`  can
contain data-context properties such as `${node.name}`.  Additionally, the
specific Service will provide some additional context properties that can be used:

* NodeExecutor will define `${exec.command}` containing the command to be executed
* FileCopier will define `${file-copy.file}` containing the local path to the file
that needs to be copied.

In addition, all of the data-context properties that are available in the
`script-args` are provided as environment variables to the 
script or interpreter when it is executed.

Environment variables are generated in all-caps with this format:

    RD_[KEY]_[NAME]

The `KEY` and `NAME` are the same as `${key.name}`. Any characters in the key or name
that are not valid Bash shell variable characters are replaced with underscore '_'.

Examples:

* `${node.name}` becomes `$RD_NODE_NAME`
* `${node.some-attribute}` becomes `$RD_NODE_SOME_ATTRIBUTE`
* `${exec.command}` becomes `$RD_EXEC_COMMAND`
* `${file-copy.file}` becomes `$RD_FILE_COPY_FILE`

### Script provider requirements

The specific service has expectations about
the way your provider script behaves:

* Exit code of 0 indicates success
* Any other exit code indicates failure

For `NodeExecutor`

:   All output to `STDOUT`/`STDERR` will be captured for the job's output

For `FileCopier`

:   The first line of output of `STDOUT` MUST be the filepath of the file copied 
    to the target node.  Other output is ignored. All output to `STDERR` will be
    captured for the job's output.

