% Plugins
% Alex Honor; Greg Schueler
% November 20, 2010

Plugins for Rundeck contain new Providers for some of the Services used by
the Rundeck core.

Rundeck comes with some built-in providers for these services, but Plugins
let you write your own, or use third-party implementations.

Rundeck currently comes installed with a few useful plugins: script-plugin and 
stub-plugin.  See [Pre-installed plugins](plugins.html#pre-installed-plugins) for more info.

## Installing Plugins

Installation of plugins is simple:

Put the plugin file, such as `plugin.jar` or `some-plugin.zip`, into the Rundeck 
server's libext dir:

    cp some-plugin.zip $RDECK_BASE/libext

The plugin is now enabled, and any providers it defines can be used by nodes
or projects.

The Rundeck server does not have to be restarted.

## Uninstalling or Updating Plugins

You can simply remove the plugin files from `$RDECK_BASE/libext` to uninstall
them.

You can overwrite an old plugin with a newer version to update it.

## About Plugins

Plugins are files that contain one or more Service Provider implementations. Each
plugin file could contain multiple Providers for different types of services,
however typically each plugin file would contain only providers related in some
fashion.

Rundeck includes a number of "built-in" providers, as well as a few 
"included" plugin files.

In this document "plugin" and "provider" are used somewhat interchangably. When 
referring to an actual file containing the provider implementations we will say
"plugin file".

![Rundeck Providers and Plugin Files](../figures/fig1102.png)

## Types of Plugins

Rundeck supports several different types of plugins to perform different kinds 
of services.

### Node Execution Plugins

These plugins define ways of executing commands on nodes, and copying files to nodes.  

More information:

* Configuration: [Node Execution Services](plugins.html#node-execution-services)
* Lifecycle: [When Node Execution Service providers are invoked](plugins.html#when-node-execution-service-providers-are-invoked)
* Built-in Providers: [Node Execution services](plugins.html#node-execution-services-1)
* Included Plugins: [Pre-installed plugins](plugins.html#pre-installed-plugins)

### Resource Model Source Plugins

These plugins define mechanisms for retrieving Resource Model information from a
specific kind of source (such as a URL, file, or set of files in a directory).

More information:

* Configuration: [Resource Model Sources](plugins.html#resource-model-sources)
* Built-in Providers: [Resource Model Sources](plugins.html#resource-model-sources-1)

### Resource Format Plugins

These plugins define parsers and generators for different document formats, and
are used by the Resource Model Source Plugins, as well as other parts of the
Rundeck system.

More information:

* Configuration: [Resource Format Generators and Parsers](plugins.html#resource-format-generators-and-parsers)
* Built-in Providers: [Resource Format services](plugins.html#resource-format-services)

## About Services and Providers

The Rundeck core makes use of several different "Services" that provide
functionality for the different steps necessary to execute workflows, jobs, 
and commands across multiple nodes.

Each Service makes use of "Providers". Each Provider has a unique "Provider Name"
that is used to identify it, and most Services have default Providers unless
you specify different ones to use.

![Rundeck Services and Providers](../figures/fig1101.png)

Services fall into different categories, which determine how and where they are used.

*Service Categories*:

1. **Node Execution services** - providers of these services operate in the context of a single Node definition, and
  can be configured at Node scope or higher:

    1. Node Executor - these providers define ways of executing a command on a Node (local or remote)
    2. File Copier - these providers define ways of copying files to a Node.

2. **Project services**

    1. Resource Model Source - (aka "Resource Providers") these define ways of retrieving Node resources for a Project 

3. **Global services** (framework level)

    1. Resource Format Parser - these define document format parsers
    2. Resource Format Generators - these define document format generators

Specifics of how providers of these plugins work is listed below.

Rundeck Plugins can contain more than one Provider.

## Using Providers

### Node Execution Services

The two *Node services*, Node Executor and File Copier, are both configured similarly.
They are configured for particular nodes on a node-specific basis,
or set as a default provider for a project or for the system.

If multiple providers are defined the most specific definition takes precedence
in this order:

1. Node specific
2. Project scope
3. Framework scope

#### Node Specific

To enable a provider for a node, add an attribute to the node definition.

*Node Executor provider attributes*:

`node-executor`

:    specifies the provider name for a non-local node.

`local-node-executor`

:    specifies the provider name for the local (server) node.


*FileCopier provider attributes*:

`file-copier`

:    specifies the provider by name for a non-local node.

`local-file-copier`

:    specifies the provider by name for the local (server) node.

Example Node in YAML specifying `stub` NodeExecutor and FileCopier:

    remotehost:
        hostname: remotehost
        node-executor: stub
        file-copier: stub

#### Project or Framework Scope

*Node Executor*

You can define the default connection providers to use for nodes at either the Project or
Framework scope (or both).  To do so, configure any of the following properties
in the `project.properties` or the `framework.properties` files.  

`service.NodeExecutor.default.provider`

:   Specifies the default NodeExecutor provider for remote nodes

`service.NodeExecutor.default.local.provider`

:   Specifies the default Node Executor provider for the  local node.

*File Copier*

`service.FileCopier.default.provider`

:   Specifies the default File Copier provider for remote nodes.

`service.FileCopier.default.local.provider`

:   Specifies the default File Copier provider for the local node.

Example `project.properties` to set default local providers to `stub`:

    service.NodeExecutor.default.local.provider=stub
    service.FileCopier.default.local.provider=stub

### Resource Model Sources

The *Resource Model Sources* providers can be configured for a single project 
in the `project.properties` file.

You can define multiple Resource Model Sources for the project, and can mix and match
the specific providers depending on your needs.

When you define multiple Source providers in a project, then the resulting set of Nodes will 
effectively be a merge of all the sources, in the order in which they are declared. This
means that if two or more Sources provide a definition of a node with the same name, then
the definition from lowest Source in the list will be used.

The order that the providers are loaded (and thus the nodes are merged) is:

1. `project.resources.file`: A File Model Source with default configuration.
2. `project.resources.url`: A URL Model Source with default configuration. (optional)
3. All `resources.source.N` configurations in order starting at 1

#### Resource Model Source configuration

The `project.properties` file for each project allows you to configure the Resource Model Sources in these ways:

* Define `project.resources.file` - this file path is used as a File Source path, with *autogeneration* and *includeServerNode* both true.
* Define `project.resources.url` - this URL is used as a URL Source url, with caching enabled

You may also define a list of more sources in this way:

Starting at index `1`, define these properties for your Source numbered `N`:

    resources.source.N.type=<provider-name>
    resources.source.N.config.<property>=<value>
    resources.source.N.config.<property2>=<value2>
    ...

Using one of the available Resource Model Source provider names for the `<provider-name>` value. For each Resource Model Source provider, 
you can specify the configuration properties for the source.

Example project.properties configuration of a default File provider, and two other providers:

    project.resources.file=/home/rundeck/projects/example/etc/resources.xml
    
    resources.source.1.type=url
    resources.source.1.url=http://server/nodes.yaml
    
    resources.source.2.type=directory
    resources.source.2.directory=/home/rundeck/projects/example/resources

### Resource Format Generators and Parsers

Resource Format Generators and Parsers define support for file formats that can
be generated from or parsed into a set of Resource Node definitions.

These are used by other parts of the system, such as the Resource Model Sources.

There is no configuration necessary to use these providers, however the specific
Provider Name that each generator and parser defines has to be known in order
to make use of the provider.  The specific Provider Name is used as the 
"format name" when you want to use the parser or generator.

For example, to enable a particular Resource Format parser to be used by a File
Resource Model Source (see [File Resource Model Source Configuration](plugins.html#file-resource-model-source-configuration)), you should specify
the Provider Name for the parser as the format for the source:

    resources.source.1.format=myformat

This would specify the use of "myformat" provider.

In other cases, the exact name of the provider may not be known (for example 
when loading content from a remote URL).  Each Generator and Parser must define
a list of MIME Type strings and file extensions that they support. These 
are used to determine which parser/generator is to be used.

## When Node Execution Service providers are invoked

Rundeck executes Command items on Nodes.  The command may be part of a Workflow as defined
in a Job, and it may be executed multiple times on different nodes.

Currently three "kinds" of Command items can be specified in Workflows:

1. "exec" commands - simple system command strings
2. "script" commands - either embedded script content, or server-local script 
files can be sent to the specified node and then executed with a set of input arguments.
3. "jobref" commands - references to other Jobs by name that will be executed with
a set of input arguments.

Rundeck uses the NodeExecutor and FileCopier services as part of the process of 
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

Rundeck uses a few built-in providers to provide the default service:

### Node Execution services

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

#### SSH Provider

The SSH Node Executor and File Copier are included as the default providers for Rundeck.

Out of the box typical node configuration to make use of these is simple. 

* Set the `hostname` attribute for the nodes.  It can be in the format "hostname:port" to indicate that a non-default port should be used. The default port is 22.
* Set the `username` attribute for the nodes to the username to connect to the remote node.
* set up public/private key authentication from the Rundeck server to the nodes

This will allow remote command and script execution on the nodes.

See below for more configuration options.

**Sudo Password Authentication**

The SSH Provider also includes support for a secondary Sudo Password Authentication. This simulates a user writing a password to the terminal into a password prompt when invoking a "sudo" command that requires password authentication.

##### Configuring SCP File Copier

In addition to the general SSH configuration mentioned for in this section, some additional configuration can be done for SCP. 

When a Script is executed on a remote node, it is copied over via SCP first, and then executed.  In addition to the SSH connection properties, these node attributes
can be configured for SCP:

* `file-copy-destination-dir`: The directory on the remote node to copy the script file to before executing it. The default value is `C:/WINDOWS/TEMP/` on Windows nodes, and `/tmp` for other nodes.
* `osFamily`: specify "windows" for windows nodes.

##### Configuring SSH Authentication type

SSH authentication can be done in two ways, via password or public/private key.

By default, public/private key is used, but this can be changed on a node, project, or framework scope.

The mechanism used is determined by the `ssh-authentication` property.  This property can have two different values:

* `password`
* `privateKey` (default)

When connecting to a particular Node, this sequence is used to determine the correct authentication mechanism:

1. **Node level**: `ssh-authentication` attribute on the Node. Applies only to the target node.
2. **Project level**: `project.ssh-authentication` property in `project.properties`.  Applies to any project node by default.
3. **Rundeck level**: `framework.ssh-authentication` property in `framework.properties`. Applies to all projects by default.

If none of those values are set, then the default public/private key authentication is used.

##### Configuring SSH Username

The username used to connect via SSH is taken from the `username` Node attribute:

* `username="user1"`

This value can also include a property reference if you want to dynamically change it, for example to the name of the current Rundeck user, or the username submitted as a Job Option value:

* `${job.username}` - uses the username of the user executing the Rundeck execution.
* `${option.someUsername}` - uses the value of a job option named "someUsername".

If the `username` node attribute is not set, then the static value provided via project or framework configuration is used. The username for a node is determined by looking for a value in this order:

1. **Node level**: `username` node attribute. Can contain property references to dynamically set it from Option or Execution values.
2. **Project level**: `project.ssh.user` property in `project.properties` file for the project.
3. **Rundeck level**: `framework.ssh.user` property in `framework.properties` file for the Rundeck installation.

##### Configuring SSH private keys

The default authentication mechanism is public/private key.

The built-in SSH connector allows the private key to be specified in several different ways.  You can configure it per-node, per-project, or per-Rundeck instance.

When connecting to the remote node, Rundeck will look for a property/attribute specifying the location of the private key file, in this order, with the first match having precedence:

1. **Node level**: `ssh-keypath` attribute on the Node. Applies only to the target node.
2. **Project level**: `project.ssh-keypath` property in `project.properties`.  Applies to any project node by default.
3. **Rundeck level**: `framework.ssh-keypath` property in `framework.properties`. Applies to all projects by default.
4. **Rundeck level**:  `framework.ssh.keypath` property in `framework.properties`. Applies to all projects by default (included for compatibility with Rundeck < 1.3). (default value: `~/.ssh/id_rsa`).

If you private key is encrypted with a passphrase, then you can use a "Secure Option" to prompt the user to enter the passphrase when executing on the Node.  See below.

##### Configuring SSH Private Key Passphrase

Using a passphrase for privateKey authentication works in the following way:

* A Job must be defined specifying a Secure Option to prompt the user for the key's passphrase.
* Target nodes must be configured to use privateKey authentication.
* When the user executes the Job, they are prompted for the key's passphrase.  The Secure Option value for the passphrase is not stored in the database, and is used only for that execution.

Therefore Private Key Passphrase authentication has several requirements and some limitations:

1. Private Key-authenticated nodes requiring passphrases can only be executed on via a defined Job, not via Ad-hoc commands (yet).
2. Each Job that will execute on such Nodes must define a Secure Option to prompt the user for the key's passphrase before execution.
3. All Nodes using passphrase protected private keys for a Job must have a matching Secure Option defined, or may use the same option name (or the default) if they share the key's passphrase (e.g. using the same private key).

Passphrases are input either via the GUI or arguments to the job if executed via CLI or API.

To enable SSH Private Key authentication, first make sure the `ssh-authentication` value is set as described in [Configuring SSH Authentication type](plugins.html#configuring-ssh-authentication-type).  Second, configure the path to the private key file, as described in [Configuring SSH private keys](plugins.html#configuring-ssh-private-keys).

Next, configure a Job, and include an Option definition where `secureInput` is set to `true`.  The name of this option can be anything you want, but the default value of `sshKeyPassphrase` assumed by the node configuration is easiest.

If the value is not `sshKeyPassphrase`, then make sure to set the following attribute on each Node for password authentication:

* `ssh-key-passphrase-option` = "`option.NAME`" where NAME is the name of the Job's secure option.

An example Node and Job option configuration are below:

    <node name="egon" description="egon" osFamily="unix"
        username="rundeck"
        hostname="egon"
        ssh-keypath="/path/to/privatekey_rsa"
        ssh-authentication="privateKey"
        ssh-password-option="option.sshKeyPassphrase" />

Job:

    <joblist>
        <job>
            ...
            <context>
              <project>project</project>
              <options>
                <option required='true' name='sshKeyPassphrase' secure='true'
                  description="Passphrase for SSH Private Key"/>
              </options>
            </context>
            ...
        </job>
    </joblist>

##### Configuring SSH Password Authentication

Password authentication works in the following way:

* A Job must be defined specifying a Secure Option to prompt the user for the password
* Target nodes must be configured for password authentication
* When the user executes the Job, they are prompted for the password.  The Secure Option value for the password is not stored in the database, and is used only for that execution.

Therefore Password authentication has several requirements and some limitations:

1. Password-authenticated nodes can only be executed on via a defined Job, not via Ad-hoc commands (yet).
2. Each Job that will execute on password-authenticated Nodes must define a Secure Option to prompt the user for the password before execution.
3. All Nodes using password authentication for a Job must have an equivalent Secure Option defined, or may use the same option name (or the default) if they share authentication passwords.

Passwords for the nodes are input either via the GUI or arguments to the job if executed via CLI or API.

To enable SSH Password authentication, first make sure the `ssh-authentication` value is set as described in [Configuring SSH Authentication type](plugins.html#configuring-ssh-authentication-type).

Next, configure a Job, and include an Option definition where `secureInput` is set to `true`.  The name of this option can be anything you want, but the default value of `sshPassword` assumed by the node configuration is easiest.

If the value is not `sshPassword`, then make sure to set the following attribute on each Node for password authentication:

* `ssh-password-option` = "`option.NAME`" where NAME is the name of the Job's secure option.

An example Node and Job option configuration are below:

    <node name="egon" description="egon" osFamily="unix"
        username="rundeck"
        hostname="egon"
        ssh-authentication="password"
        ssh-password-option="option.sshPassword1" />

Job:

    <joblist>
        <job>
            ...
            <context>
              <project>project</project>
              <options>
                <option required='true' name='sshPassword1' secure='true' />
              </options>
            </context>
            ...
        </job>
    </joblist>


##### Configuring Secondary Sudo Password Authentication

The SSH provider supports a secondary authentication mechanism: Sudo password authentication.  This is useful if your security requirements are such that you require the SSH connection to be under a specific user's account instead of a generic "rundeck" account, and you still need to allow "sudo" level commands to be executed requiring a password to be entered.

This works in the following way:

* On Job execution, the user is prompted to enter a Sudo password
* After connecting to the remote node via SSH, a command requiring "sudo" authentication is issued, such as "sudo -u otheruser /sbin/some-command"
* The remote node will prompt for a sudo password, expecting user input
* The SSH Provider will write the password to the remote node
* The sudo command will execute as if a user had entered the command

Similarly to SSH Password authentication, Sudo Password Authentication requires:

* A Job must be defined specifying a Secure Option to prompt the user for the password
* Target nodes must be configured for Sudo authentication
* When the user executes the Job, they are prompted for the password.  The Secure Option value for the password is not stored in the database, and is used only for that execution.

Therefore Sudo Password Authentication has several requirements and some limitations:

1. Sudo Password authenticated nodes can only be executed on via a defined Job, not via Ad-hoc commands (yet).
2. Each Job that will execute on Sudo Password Authenticated Nodes must define a Secure Option to prompt the user for the Sudo password before execution.
3. All Nodes using Sudo password authentication for a Job must have an equivalent Secure Option defined, or may use the same option name (or the default) if they share sudo authentication passwords.

Passwords for the nodes are input either via the GUI or arguments to the job if executed via CLI or API.

To enable Sudo Password Authentication, set the `sudo-command-enabled` property/attribute to `true`.

You can configure the way the Sudo Password Authentication works by setting these properties at the Node, Project or Rundeck scopes. Simply set the attribute name on a Node, the `project.NAME` in project.properties, or `framework.NAME` in framework.properties:
 
* `sudo-command-enabled` - set to "true" to enable Sudo Password Authentication.
* `sudo-command-pattern` - a regular expression to detect when a command execution should expect to require Sudo authentication. Default pattern is `^sudo$`.
* `sudo-password-option` - an option reference ("option.NAME") to define which secure option value to use as password.  The default is `option.sudoPassword`.
* `sudo-prompt-pattern` - a regular expression to detect the password prompt for the Sudo authentication. The default pattern is `^\[sudo\] password for .+: .*`
* `sudo-failure-pattern` - a regular expression to detect the password failure response.  The default pattern is `^.*try again.*`.
* `sudo-prompt-max-lines` - maximum lines to read when expecting the password prompt. (default: `12`).
* `sudo-prompt-max-timeout` - maximum milliseconds to wait for input when expecting the password prompt. (default `5000`)
* `sudo-response-max-lines` - maximum lines to read when looking for failure response. (default: `2`).
* `sudo-response-max-timeout` - maximum milliseconds to wait for response when detecting the failure response. (default `5000`)
* `sudo-fail-on-prompt-max-lines` - true/false. If true, fail execution if max lines are reached looking for password prompt. (default: `false`)
* `sudo-success-on-prompt-threshold` - true/false. If true, succeed (without writing password), if the input max lines are reached without detecting password prompt. (default: `true`).
* `sudo-fail-on-prompt-timeout` - true/false. If true, fail execution if timeout reached looking for password prompt. (default: `true`)
* `sudo-fail-on-response-timeout` - true/false. If true, fail on timeout looking for failure message. (default: `false`)

Note: the default values have been set for the unix "sudo" command, but can be overridden if you need to customize the interaction.

Next, configure a Job, and include an Option definition where `secureInput` is set to `true`.  The name of this option can be anything you want, but the default value of `sudoPassword` recognized by the plugin can be used.

If the value is not `sudoPassword`, then make sure to set the following attribute on each Node for password authentication:

* `sudo-password-option` = "`option.NAME`" where NAME is the name of the Job's secure option.

An example Node and Job option configuration are below:

    <node name="egon" description="egon" osFamily="unix"
        username="rundeck"
        hostname="egon"
        sudo-command-enabled="true"
        sudo-password-option="option.sudoPassword2" />

Job:

    <joblist>
        <job>
             <sequence keepgoing='false' strategy='node-first'>
              <command>
                <exec>sudo apachectl restart</exec>
              </command>
            </sequence>

            <context>
              <project>project</project>
              <options>
                <option required='true' name='sudoPassword2' secure='true' description="Sudo authentication password"/>
              </options>
            </context>
            ...
        </job>
    </joblist>

##### Configuring Multiple Sudo Password Authentication

You can enable a further level of sudo password support for a node.  If you have
the requirement of executing a chain of "sudo" commands, such as "sudo -u user1
sudo -u user2 command", and need to enable password input for both levels of
sudo.  This is possible by configuring a secondary set of properties for your
node/project/framework.

The configuration properties are the same as those for the first-level of sudo
password authentication described in [Configuring Secondary Sudo Password
Authentication](#configuring-secondary-sudo-password-authentication), but with a
prefix of "sudo2-" instead of "sudo-", such as:

    sudo2-command-enabled="true"
    sudo2-command-pattern="^sudo .+? sudo .*$"

This would turn on a mechanism to expect and respond to another sudo password
prompt when the command matches the given pattern.

If a value for "sudo2-password-option" is not set, then a default value of
`option.sudo2Password` will be used.

**A note about the "sudo2-command-pattern":**

The sudo authentication mechanism uses two regular expressions to test whether it should be 
invoked.

For the first sudo authentication, the "sudo-command-pattern" value is matched against
the **first component of the command being executed**. The default value for this pattern is `^sudo$`.
So a command like "sudo -u user1 some command" will match correctly.  You can modify the 
regular expression (e.g. to support "su"), but it will always only match against the first 
part of the command.

If "sudo2-command-enabled" is "true", then the "sudo2-command-pattern" is also checked 
and if it matches then another sudo authentication is enabled.
However this regular expression is tested against the **entire command string**
to make it possible to determine whether it should be enabled. The default value is 
`^sudo .+? sudo .*$`. If necessary you should customize the value.

### Resource Model Sources

Rundeck includes these built-in providers in the core installation:

`file`

:    Uses a file on the file system, in any of the supported Resources formats.

`url`

:    GETs a URL, and expects one of the supported Resources formats.

`directory`

:    looks at all files in a directory for suppored file extensions, and internally uses the `file` provider for
     each file that matches.

`script`

:    Executes a script and parses the output as one of the supported formats

To configure these providers, refer to [Resource Model Source configuration](plugins.html#resource-model-source-configuration) and use the following configuration properties.

#### File Resource Model Source Configuration

The `file` Resource Model Source provider reads a file in one of the supported
[Resource Model Document Formats](rundeck-basics.html#resource-model-document-formats).

Name                          Value                           Notes
-----                         ------                          ------
`file`                        file path                       Path to a file on disk.
`format`                      format name                     Can be used to declare the format explicitly. Otherwise the format is determined from the `file`'s extension.
`requireFileExists`           true/false                      If true and the file is missing, causes a failure to load the nodes. (Default: false)
`includeServerNode`           true/false                      If true, include the Project's server node automatically. (Default: false)
`generateFileAutomatically`   true/false                      If true, create the file automatically if it is missing. (Default: false)
----------------------------

Table: Configuration properties for `file` Resource Model Source provider

The value of `format` must be one of the supported [Resource Model Document Formats](rundeck-basics.html#resource-model-document-formats). The built-in formats are: `resourcexml` or `resourceyaml`, but any format provided by a [Resource Format Plugin](plugins.html#resource-format-plugins) can be specified as well.

*Example:*

    resources.source.1.type=file
    resources.source.1.file=/home/rundeck/projects/example/etc/resources2.xml
    resources.source.1.format=resourcexml
    resources.source.1.requireFileExists=true
    resources.source.1.includeServerNode=true
    resources.source.1.generateFileAutomatically=true

#### URL Resource Model Source Configuration

The `url` Resource Model Source provider performs a HTTP GET request to retrieve the Nodes definition.

Configuration properties:

Name      Value       Notes
-----     ------      ------
`url`     URL         A valid URL, either `http:`, `https:` or `file:` protocol.
`cache`   true/false  If true, use ETag/Last-Modified information from the server to only download new content if it has changed. If false, always download the content. (Default: true)
`timeout` seconds     Number of seconds before request fails due to timeout. `0` means no timeout. (Default: 30) 
----------------------------

Table: Configuration properties for `url` Resource Model Source provider

The [Resource Model Document Format](rundeck-basics.html#resource-model-document-formats) that is used is determined by the MIME type
sent by the remote server. The built-in formats accept "\*/xml" and "\*/yaml" and "*/x-yaml". See [Resource Format Plugin](plugins.html#resource-format-plugins).

*Example:*

    resources.source.1.type=url
    resources.source.1.url=file:/home/rundeck/projects/example/etc/resources2.xml
    resources.source.1.cache=true
    resources.source.1.timeout=0

#### Directory Resource Model Source Configuration

The `directory` Resource Model Source provider lists all files in a directory, and loads each one that has a supported file extension
as File Resource Model Source with all default configuration options.

Name                          Value                           Notes
-----                         ------                          ------
`directory`                   directory path                  All files in the directory that have a supported file extension will be loaded
----------------------------

Table: Configuration properties for `directory` Resource Model Source provider

*Example:*

    resources.source.2.type=directory
    resources.source.2.directory=/home/rundeck/projects/example/resources
    
#### Script Resource Model Source Configuration

The `script` Resource Model Source provider executes a script file and reads
the output of the script as one of the supported [Resource Model Document Formats](rundeck-basics.html#resource-model-document-formats).

Name             Value                           Notes
-----            ------                          ------
`file`           Script file path                If required by the `interpreter`, the file should be executable
`interpreter`    Command or interpreter to use   e.g. "bash -c"
`args`           Additional arguments to pass    The arguments will be added after the script file name to the executed commandline
`format`         Format name                     Must be used to declare the format explicitly.
----------------------------

Table: Configuration properties for `script` Resource Model Source provider

The script will be executed in this way:

    [interpreter] file [args]

All output on STDOUT will be passed to a Resource Format Parser to parse.  The
format specified must be available.

*Example:*

    resources.source.2.type=script
    resources.source.2.file=/home/rundeck/projects/example/etc/generate.sh
    resources.source.2.interpreter=bash -c
    resources.source.2.args=-project example
    resources.source.2.format=resourceyaml

### Resource Format services

Resource Format services (Generators and Parsers) typically come in matched 
pairs, with both a parser and generator for the same format name.

Rundeck includes these built-in providers in the core installation:

`resourcexml`

:    Supports the Resource XML document format: [resource-v13(5) XML](../manpages/man5/resource-v13.html).

    Supported MIME types:

    * Generator: "text/xml"
    * Parser: "*/xml"

    Supported File extensions:

    * ".xml"

`resourceyaml`

:    Supports the Resource YAML document format: [resource-v13(5) YAML](../manpages/man5/resource-yaml-v13.html).

    Supported MIME types:

    * Generator: "text/yaml", "text/x-yaml", "application/yaml", "application/x-yaml"
    * Parser: "\*/yaml", "\*/x-yaml"

    Supported File extensions:

    * ".yml", ".yaml"

## Pre-installed plugins

Rundeck comes with two pre-installed plugins that may be useful, and also serve
as examples of plugin development and usage.

### script-plugin

The `script-plugin` includes these providers:

* `script-exec` for the NodeExecutor service
* `script-copy` for the FileCopier service

(Refer to [Using Providers](plugins.html#using-providers) to enable them.)

This plugin provides the ability to specify an external script or command
to perform a remote or local execution of a Rundeck command, and remote or local file copies.

It can be a replacement for the built-in SSH-based remote execution and SCP-based file-copy mechanism to
allow you to user whatever external mechanism you wish.

Note: this plugin offers similar functionality to the 
[Script Plugin Development](../developer/plugin-development.html#script-plugin-development) 
 model.  You may want to use this plugin to test your scripts, and
then later package them into a standalone plugin using that model.  

#### Configuring script-exec

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

    mynode:
        node-executor: script-exec
        script-exec: /bin/execremote -host ${node.hostname} -user ${node.username} -- ${exec.command}

If the command you want to run requires special handling (such as quoting or other interpretation) you may want to have a shell execute it. In which case you could specify the shell to use:

    mynode:
        node-executor: script-exec
        script-exec-shell: bash -c
        script-exec: ssh -o "some quoted option" ${node.username}@${node.hostname} ${exec.command}

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

    mynode:
        file-copier: script-copy
        script-copy: /bin/copyremote -host ${node.hostname} -user ${node.username} -- ${file-copy.file} ${node.destdir}

At run time, the properties specified would be expanded to the values for the
specific node and command string to execute.

OR, you could specify a default to apply to all nodes within the project.properties
file located at `$RDECK_BASE/projects/NAME/etc/project.properties`.

    script-copy.default.command= /bin/copyremote -host ${node.hostname} -user ${node.username} -- ${file-copy.file} ${node.destdir}

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

    mynode:
        file-copier: script-copy
        script-copy: /bin/copyremote -host ${node.hostname} -user ${node.username} -- ${file-copy.file} ${node.destdir}
        script-copy-remote-filepath: ${node.destdir}/${file-copy.filename}

At run time, the properties specified would be expanded to the values for the
specific node and command string to execute.

OR, you could specify a default to apply to all nodes within the project.properties
file located at `$RDECK_BASE/projects/NAME/etc/project.properties`.

    script-copy.default.remote-filepath= ${node.destdir}/${file-copy.filename}

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

**Example system ssh replacement**:

This example uses the system's "ssh" and "scp" commands to perform node execution 
and file copying, and doesn't make use of an external script file:

Node-only configuration:

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

    mynode:
        hostname: mynode
        username: user1
        destdir: /tmp

### stub-plugin

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

## Plugin Development

Plugins can be developed easily using scripts, or you can use Java.

See more information in the [Developer Guide - Plugin Development](../developer/plugin-development.html) chapter.
