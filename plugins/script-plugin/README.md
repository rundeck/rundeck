script plugin for Rundeck
==============================

**Note: Rundeck plugin support is currently in development and subject
to change**

This plugin provides the ability to specify an external script or command
to perform a remote or local execution of a Rundeck command, and remote or local file copies.

This plugin defines two new providers:

* 'script-exec' for the NodeExecutor service
* 'script-copy' for the FileCopier service

It can be a replacement for the built-in SSH-based remote execution and SCP-based file-copy mechanism to
allow you to user whatever external mechanism you wish.


Building
--------

Execute:

    ./gradlew
OR
    ./gradlew.bat

To build the plugin. It will produce a jar file called:

    build/libs/rundeck-script-plugin-1.2.0.jar

Install
--------

Copy the `rundeck-script-plugin-1.2.0.jar` to the RunDeck server's
libext dir:

    cp build/libs/rundeck-script-plugin-1.2.0.jar \
        $RDECK_BASE/libext

enabling script-exec NodeExecutor
--------------------

To enable the script-exec provider for a node, modify your resources.xml or resources.yaml file.

For XML:

    <node name="mynode" ...>
        <!-- to enable on a remote node -->
        <attribute name="node-executor" value="script-exec"/>
        <!-- to enable on the server node -->
        <attribute name="local-node-executor" value="script-exec"/>
    </node>

For YAML:

    mynode:
        hostname: ...
        node-executor: script-exec
        local-node-executor: script-exec

Note that `local-node-executor` is required instead of `node-executor` only if 
the node is the local (server) node.

This sets the script plugin as the "Node Executor" for the node.

### Enabling for all nodes

You can set 'script-exec' as the NodeExecutor for all nodes or for all nodes in a project by adding this
property to either `framework.properties` or `project.properties`:

    service.NodeExecutor.default.provider=script-exec


enabling script-copy FileCopier
--------------------

To enable the script-copy provider for a node, modify your resources.xml or resources.yaml file.

For XML:

    <node name="mynode" ...>
        <!-- to enable on a remote node -->
        <attribute name="file-copier" value="script-copy"/>
        <!-- to enable on the server node -->
        <attribute name="local-file-copier" value="script-copy"/>
    </node>

For YAML:

    mynode:
        hostname: ...
        file-copier: script-copy
        local-file-copier: script-copy

Note that `local-file-copier` is required instead of `file-copier` only if
the node is the local (server) node.

This enables the script plugin as the "File Copier" for the node.

### Enabling for all nodes

You can set 'script-copy' as the FileCopier for all nodes or for all nodes in a project by adding this
property to either `framework.properties` or `project.properties`:

    service.FileCopier.default.provider=script-copy

Configuring script-exec
-----------------

To configure the plugin you must specify a commandline string to execute.  Optionally
you may specify a directory to be used as the working directory when executing
the commandline string.

You can configure these across all projects (framework-wide), a single project 
(project-wide), or specifically for each node, with the most specific configuration
value taking precedence.

### Configuring the command for script-exec

For Framework and Project-wide, configure a property in either the framework.properties or 
project.properties files:

`plugin.script-exec.default.command`

:   Specifies the default system command to run

For node-specific add an attribute named `script-exec` to the node.

`script-exec`

:   Specifies the system command to run

See [Defining the script-exec command](#defining-the-script-exec-command) for
what to specify for this property.

### Configuring the working directory

For Framework and Project-wide, configure a property in either the framework.properties or 
project.properties files:

`plugin.script-exec.default.dir`

:   Specifies the default woring directory for the execution


For node-specific add an attribute named `script-exec-dir` to the node.

`script-exec-dir`

:   Specifies the default woring directory for the execution (optional)


###Defining the script-exec command

The value of this property or attribute should be the complete commandline 
string to execute in an external system process.

You can use *Data context properties* as you can in normal Rundeck command 
execution, such as `${node.name}` or `${job.name}`. 

In addition, the plugin provides these new data context properties:

`exec.command`

:   The command that the workflow/user has specified to run on the node


Example:

If you wanted to run some external remote connection command ("/bin/execremote") in lieu of the 
built-in ssh command, you could specify these attributes for node:

    mynode:
        node-executor: script-exec
        script-exec: /bin/execremote -host ${node.hostname} -user ${node.username} -- ${script-exec.command}

At run time, the properties specified would be expanded to the values for the
specific node and command string to execute.

OR, you could specify a default to apply to all nodes within the project.properties 
file located at `$RDECK_BASE/projects/NAME/etc/project.properties`.

    script-exec.default.command= /bin/execremote -host ${node.hostname} \
        -user ${node.username} -- ${script-exec.command}

Similarly for the `$RDECK_BASE/etc/framework.properties` file to apply to all
projects.

### Requirements for the script-exec command

The command run by by the script plugin is expected to behave in the following manner:

* Exit with a system exit code of "0" in case of success.
* Any other exit code indicates failure

Note: all output from STDOUT and STDERR will be captured as part of the Rundeck job execution.

Configuring script-copy
-----------------

To configure script-copy you must specify a commandline string to execute.  Optionally
you may specify a directory to be used as the working directory when executing
the commandline string.

You can configure these across all projects (framework-wide), a single project
(project-wide), or specifically for each node, with the most specific configuration
value taking precedence.

### Configuring the command for script-copy

For Framework and Project-wide, configure a property in either the framework.properties or
project.properties files:

`plugin.script-copy.default.command`

:   Specifies the default system command to run

For node-specific add an attribute named `script-copy` to the node.

`script-copy`

:   Specifies the system command to run

See [Defining the script-copy command](#defining-the-script-copy-command) for
what to specify for this property.

### Configuring the working directory

For Framework and Project-wide, configure a property in either the framework.properties or
project.properties files:

`plugin.script-copy.default.dir`

:   Specifies the default woring directory for the execution


For node-specific add an attribute named `script-copy-dir` to the node.

`script-copy-dir`

:   Specifies the default woring directory for the execution (optional)


## Defining the script-copy command

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
        script-exec: /bin/execremote -host ${node.hostname} -user ${node.username} -- ${script-exec.command}

At run time, the properties specified would be expanded to the values for the
specific node and command string to execute.

OR, you could specify a default to apply to all nodes within the project.properties
file located at `$RDECK_BASE/projects/NAME/etc/project.properties`.

    script-exec.default.command= /bin/execremote -host ${node.hostname} \
        -user ${node.username} -- ${script-exec.command}

Similarly for the `$RDECK_BASE/etc/framework.properties` file to apply to all
projects.

### Requirements of script-copy command

The command executed by script-copy is expected to behave in the following manner:

* Output the filepath of the copied file on the target node as the first line of output on STDOUT
* Exit with an exit code of "0" to indicate success
* Exit with any other exit code indicates failure

Example Scripts
===========

Here are some example scripts to show the some possible usage patterns.

Example script-exec
-----------

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

Example script-copy
------

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
