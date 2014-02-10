
## Script plugins

Rundeck comes with two pre-installed plugins that may be useful, and also serve
as examples of plugin development and usage.

The `script-plugin` includes these providers:

* `script-exec` for the NodeExecutor service
* `script-copy` for the FileCopier service

(Refer to [Using plugins](plugins.html#using-providers) to enable them.)

This plugin provides the ability to specify an external script or command
to perform a remote or local execution of a Rundeck command, and remote or local file copies.

It can be a replacement for the built-in SSH-based remote execution and SCP-based file-copy mechanism to
allow you to user whatever external mechanism you wish.

Note: this plugin offers similar functionality to the 
[Script Plugin Development](../developer/plugin-development.html#script-plugin-development) 
 model.  You may want to use this plugin to test your scripts, and
then later package them into a standalone plugin using that model.  

### Configuring script-exec

To configure the plugin you must specify a commandline string to execute.  Optionally
you may specify a directory to be used as the working directory when executing
the commandline string, and a shell to invoke the command.

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

See [Defining the script-exec command](plugins.html#defining-the-script-exec-command) for
what to specify for this property.

#### Configuring the working directory

For Framework and Project-wide, configure a property in either the framework.properties or 
project.properties files:

`plugin.script-exec.default.dir`

:   Specifies the default working directory for the execution

For node-specific add an attribute named `script-exec-dir` to the node.

`script-exec-dir`

:   Specifies the default working directory for the execution (optional)

#### Configuring the shell

For Framework and Project-wide, configure a property in either the framework.properties or 
project.properties files:

`plugin.script-exec.default.shell`

:   Specifies the shell to use to interpret the command, e.g. "bash -c" or "cmd.exe /c"

For node-specific add an attribute named `script-exec-shell` to the node.

`script-exec-shell`

:   Specifies the shell to use to interpret the command, e.g. "bash -c" or "cmd.exe /c" (optional)

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

~~~~~~~~ {.yaml}
mynode:
    node-executor: script-exec
    script-exec: /bin/execremote -host ${node.hostname} -user ${node.username} -- ${exec.command}
~~~~~~~~~~~

If the command you want to run requires special handling (such as quoting or other interpretation) you may want to have a shell execute it. In which case you could specify the shell to use:

~~~~~~~~~ {.yaml}
mynode:
    node-executor: script-exec
    script-exec-shell: bash -c
    script-exec: ssh -o "some quoted option" ${node.username}@${node.hostname} ${exec.command}
~~~~~~~~~~~~

At run time, the properties specified would be expanded to the values for the
specific node and command string to execute.

OR, you could specify a default to apply to all nodes within the project.properties 
file located at `$RDECK_BASE/projects/NAME/etc/project.properties`.

    script-exec.default.command=/bin/execremote -host ${node.hostname} \
        -user ${node.username} -- ${exec.command}

Similarly for the `$RDECK_BASE/etc/framework.properties` file to apply to all
projects.

#### Requirements for the script-exec command

The command run by by the script plugin is expected to behave in the following manner:

* Exit with a system exit code of "0" in case of success.
* Any other exit code indicates failure

Note: all output from STDOUT and STDERR will be captured as part of the Rundeck job execution.

### Configuring script-copy

To configure script-copy you must specify a commandline string to execute.  Optionally
you may specify a directory to be used as the working directory when executing
the commandline string, and a shell to use to interpret the command.  

You must also specify the filepath on the target node where the copied file will be placed, which can be done in two different ways.

You can configure these across all projects (framework-wide), a single project
(project-wide), or specifically for each node, with the most specific configuration
value taking precedence.

#### Configuring the command for script-copy

For Framework and Project-wide, configure these properties in either the framework.properties or
project.properties files:

`plugin.script-copy.default.command`

:   Specifies the default system command to run

For node-specific add these attributes to the node.

`script-copy`

:   Specifies the system command to run

See [Defining the script-copy command](plugins.html#defining-the-script-copy-command) for
what to specify for this property.

#### Configuring the working directory

For Framework and Project-wide, configure a property in either the framework.properties or
project.properties files:

`plugin.script-copy.default.dir`

:   Specifies the default working directory for the execution


For node-specific add an attribute named `script-copy-dir` to the node.

`script-copy-dir`

:   Specifies the default working directory for the execution (optional)

#### Configuring the shell

For Framework and Project-wide, configure a property in either the framework.properties or
project.properties files:

`plugin.script-copy.default.shell`

:   Specifies the shell to run the command (optional)

For node-specific add an attribute named `script-copy-shell` to the node.

`script-copy-shell`

:   Specifies the shell to run the command (optional)

#### Configuring the remote filepath

For Framework and Project-wide, configure a property in either the framework.properties or
project.properties files:

`plugin.script-copy.default.remote-filepath`

:   Specifies the full path of the copied file.

For node-specific add an attribute named `script-copy-remote-filepath` to the node.

`script-copy-remote-filepath`

:   Specifies the full path of the copied file.

See [Defining the script-copy filepath](plugins.html#defining-the-script-copy-filepath) for
what to specify for this property.

#### Defining the script-copy command

The value of this property or attribute should be the complete commandline
string to execute in an external system process.

You can use *Data context properties* as you can in normal Rundeck command
execution, such as `${node.name}` or `${job.name}`.

In addition, the plugin provides these new data context properties:

`file-copy.file`

:   The local filepath that should be copied to the remote node

`file-copy.filename`

:   The name of the file without any path information.

Example:

If you wanted to run some external remote connection command ("/bin/copyremote") in lieu of the
built-in SCP command, you could specify these attributes for node:

~~~~~~~~~ {.yaml}
mynode:
    file-copier: script-copy
    script-copy: /bin/copyremote -host ${node.hostname} -user ${node.username} -- ${file-copy.file} ${node.destdir}
~~~~~~~~~~~~~~~

At run time, the properties specified would be expanded to the values for the
specific node and command string to execute.

OR, you could specify a default to apply to all nodes within the project.properties
file located at `$RDECK_BASE/projects/NAME/etc/project.properties`.

    script-copy.default.command=/bin/copyremote -host ${node.hostname} -user ${node.username} -- ${file-copy.file} ${node.destdir}

Similarly for the `$RDECK_BASE/etc/framework.properties` file to apply to all
projects.

#### Defining the script-copy filepath

The value of this property or attribute should be the complete filepath on
the target node where the copied file is placed. This is to tell the FileCopier service where the remote file exists after your script copies it over, so that it
can later be executed.

You can do this in *two* ways, either as a configuration property as described here, or via output from your script, as described under [Requirements of script-copy command](plugins.html#requirements-of-script-copy-command).

You can use *Data context properties* as you can in normal Rundeck command
execution, such as `${node.name}` or `${job.name}`.

In addition, the plugin provides these new data context properties:

`file-copy.file`

:   The local filepath that should be copied to the remote node

`file-copy.filename`

:   The name of the file without any path information.

Example:

Using the "/bin/copyremote" example from above, we need to set the `script-copy-remote-filepath` to the location on the remote node where the file is copied.  Our example copies `${file-copy.file}` to the location `${node.destdir}`.  This is an attribute on the Node that we assume to be configured with a directory path.

We need to set the `script-copy-remote-filepath` to the location on the remote node where
the file will exist after being copied.  We know the filename of the file is available as `${file-copy.filename}`,  so we set it to `${node.destdir}/${file-copy.filename}`:

~~~~~~ {.yaml}
mynode:
    file-copier: script-copy
    script-copy: /bin/copyremote -host ${node.hostname} -user ${node.username} -- ${file-copy.file} ${node.destdir}
    script-copy-remote-filepath: ${node.destdir}/${file-copy.filename}
~~~~~~~~~~~~~

At run time, the properties specified would be expanded to the values for the
specific node and command string to execute.

OR, you could specify a default to apply to all nodes within the project.properties
file located at `$RDECK_BASE/projects/NAME/etc/project.properties`.

    script-copy.default.remote-filepath=${node.destdir}/${file-copy.filename}

Similarly for the `$RDECK_BASE/etc/framework.properties` file to apply to all
projects.

#### Requirements of script-copy command

The command executed by script-copy is expected to behave in the following manner:

* Exit with an exit code of "0" to indicate success
* Exit with any other exit code indicates failure
* **Either**
    * Output the filepath of the copied file on the target node as the first line of output on STDOUT
    OR
    * Define the "remote-filepath" as described above

#### Example Scripts

Here are some example scripts to show the some possible usage patterns.

**Example script-exec**:

Node definition:

~~~~~ {.yaml}
mynode:
    node-executor: script-exec
~~~~~~~~~

Project config `project.properties` file:

    plugin.script-exec.default.command=/tmp/myexec.sh ${node.hostname} ${node.username} -- ${exec.command}

Contents of `/tmp/myexec.sh`:

~~~~~~~~~ {.bash .numberLines}
#!/bin/bash

# args are [hostname] [username] -- [command to exec...]

host=$1; shift
user=$1; shift
printf -v commands '%q ' "$@"

REMOTECMD=ssh

exec "$REMOTECMD" "$user@$host" "$command"
~~~~~~~~~~~~

**Example script-copy**:

Node definition:

~~~~~~ {.yaml}
mynode:
    file-copier: script-copy
    destdir: /some/node/dir
~~~~~~~~~~

System-wide config in `framework.properties`:

    plugin.script-copy.default.command=/tmp/mycopy.sh ${node.hostname} ${node.username} ${node.destdir} ${file-copy.file}

Contents of `/tmp/mycopy.sh`:

~~~~~~~~~ {.bash .numberLines}
#!/bin/bash

# args are [hostname] [username] [destdir] [filepath]

host=$1; shift
user=$1; shift
dir=$1; shift
file=$1

name=${file##*/}

# copy to node
CPCMD=scp

"$CPCMD" "$file" "$user@$host:$dir/$name" >/dev/null || exit $?

echo "$dir/$name"
~~~~~~~~~~~~

**Example system ssh replacement**:

This example uses the system's "ssh" and "scp" commands to perform node execution 
and file copying, and doesn't make use of an external script file:

Node-only configuration:

~~~~~~~~~ {.yaml .numberLines}
mynode:
    hostname: mynode
    username: user1
    node-executor: script-exec
    script-exec: ssh -o "StrictHostKeyChecking no" ${node.username}@${node.hostname} ${exec.command}
    script-exec-shell: bash -c
    file-copier: script-copy
    destdir: /tmp
    script-copy-shell: bash -c
    script-copy: scp ${file-copy.file} ${node.username}@${node.hostname}:${node.destdir}
    script-copy-remote-filepath: ${node.destdir}/${file-copy.filename}
~~~~~~~~~~~

This could all be set as defaults in the project.properties file, such as:

    # set default node executor
    service.NodeExecutor.default.provider=script-exec

    # set script-exec defaults
    plugin.script-exec.default.command=ssh -o "StrictHostKeyChecking no" ${node.username}@${node.hostname} ${exec.command}
    plugin.script-exec.default.shell=bash -c

    #set default file copier
    service.FileCopier.default.provider=script-copy

    #set script-copy defaults
    plugin.script-copy.default.command=scp ${file-copy.file} ${node.username}@${node.hostname}:${node.destdir}
    plugin.script-copy.default.shell: bash -c
    plugin.script-copy.default.remote-filepath: ${node.destdir}/${file-copy.filename}

In which case your node definitions could be as simple as:

~~~~~~ {.yaml}
mynode:
    hostname: mynode
    username: user1
    destdir: /tmp
~~~~~~~~~~

## Stub plugin

The `stub-plugin` includes these providers:

* `stub` for the NodeExecutor service
* `stub` for the FileCopier service

(Refer to [Using Providers](plugins.html#using-providers) to enable them.)

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

