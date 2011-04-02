script-exec plugin for Rundeck
==============================

**Note: Rundeck plugin support is currently in development and subject
to change**

This plugin provides the ability to specify an external script or command
to perform a remote or local execution of a Rundeck command.

It can be a replacement for the built-in SSH-based remote execution mechanism to
allow you to user whatever external mechanism you wish.


Building
--------

Execute:

    ./gradlew
OR
    ./gradlew.bat

To build the plugin. It will produce a jar file called:

    build/libs/rundeck-script-exec-plugin-1.2.0.jar

Install
--------

Copy the `rundeck-script-exec-plugin-1.2.0.jar` to the RunDeck server's
webapp lib dir:

    cp build/libs/rundeck-script-exec-plugin-1.2.0.jar \
        $RDECK_BASE/server/exp/webapp/WEB-INF/lib

2: Modify the framework.properties file, to enable the plugin

Add this line:

    framework.plugins.NodeExecutor.classnames=com.dtolabs.rundeck.plugin.scriptexecutor.ExternalScriptExecutor


enabling the plugin
--------------------

To enable the plugin on a node, modify your resources.xml or resources.yaml file.

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

This enables the plugin as the "Node Executor" for the node.

Configuring the plugin
-----------------

To configure the plugin you must specify a commandline string to execute.  Optionally
you may specify a directory to be used as the working directory when executing
the commandline string.

You can configure these across all projects (framework-wide), a single project 
(project-wide), or specifically for each node, with the most specific configuration
value taking precedence.

### Configuring the command

For Framework and Project-wide, configure a property in either the framework.properties or 
project.properties files:

`script-exec.default.command` 

:   Specifies the default system command to run

For node-specific add an attribute named `script-exec` to the node.

`script-exec`

:   Specifies the system command to run

See [Defining the script-exec command](#defining-the-script-exec-command) for
what to specify for this property.

### Configuring the working directory

For Framework and Project-wide, configure a property in either the framework.properties or 
project.properties files:

`script-exec.default.dir` 

:   Specifies the default woring directory for the execution


For node-specific add an attribute named `script-exec-dir` to the node.

`script-exec-dir`

:   Specifies the default woring directory for the execution (optional)


Defining the script-exec command
-----------------

The value of this property or attribute should be the complete commandline 
string to execute in an external system process.

You can use *Data context properties* as you can in normal Rundeck command 
execution, such as `${node.name}` or `${job.name}`. 

In addition, the plugin provides these new data context properties:

`script-exec.command`

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