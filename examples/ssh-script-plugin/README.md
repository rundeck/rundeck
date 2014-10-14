ssh-example-1.0-plugin
------

Example script plugin that uses the system local "ssh" and "scp" commands
to execute commands and copy files to remote nodes.

Build
====

    make

produces:

    ssh-example-1.0-plugin.zip

Files
=====

`ssh-example-1.0-plugin/plugin.yaml`

:   Defines the metadata for the plugin

`ssh-example-1.0-plugin/ssh-exec.sh`

:   Script used to execute ssh for remote commands

`ssh-example-1.0-plugin/ssh-copy.sh`

:   Script used to execute scp for remote file copies


Plugin metadata
=====

The `plugin.yaml` file declares two script based service providers, for 
the NodeExecutor and FileCopier services.

Usage
=====

Install the plugin in your `$RDECK_BASE/libext` directory:

    mv ssh-example-1.0-plugin.zip $RDECK_BASE/libext

Configure the `node-executor` and/or the `file-copier` attributes for your nodes:

    <node name="mynode" hostname="mynode" username="username">
        <attribute name="node-executor" value="ssh-example"/>
        <attribute name="file-copier" value="scp-example"/>

        <!-- 
            See below for more info on supported node attributes
        -->
        <attribute name="ssh-port" value="23"/>
        <attribute name="ssh-keyfile" value="/path/to/key.id_*sa"/>
        <attribute name="ssh-test" value="true"/>
    </node>


Custom Node Attributes
======

You can alter behavior of the ssh-example plugin by adding attributes to your nodes.

`ssh-port`

:   Specify port to connect to. If not set, 22 will be used.

`ssh-keyfile`

:   Specify identify keyfile to use. If not set, the default behavior of `ssh/scp` command will be used.

`ssh-test`

:   If set to "true" then a dry-run of the command will be echoed but the command will not be run.

`ssh-opts`

:   Any custom SSH commandline options can be specified here.  E.g.: `ssh-opts="-o ConnectTimeout=20"`
    will set the `ConnectTimeout` SSH option to 20 seconds.

### SCP specific

`scp-dir`

:   Directory on the target node to copy the script files to.

`scp-opts`

:   If set, will override any value of `ssh-opts`, and can be used to specify custom opts for scp.
