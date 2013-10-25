% Configuration


## Configuration layout

Configuration file layout differs between the RPM and Launcher
installation methods. See [RPM layout](configuration.html#rpm-layout) and
[Launcher layout](configuration.html#launcher-layout) for details.

### RPM layout

    /etc/rundeck
    |-- admin.aclpolicy
    |-- framework.properties
    |-- log4j.properties
    |-- profile
    |-- project.properties
    |-- jaas-loginmodule.conf
    |-- log4j.properties
    |-- realm.properties
    |-- rundeck-config.properties
    `-- ssl
        |-- ssl.properties
        |-- keystore (not packaged)
        `-- truststore (not packaged)

### Launcher layout

    $RDECK_BASE/etc
    |-- admin.aclpolicy
    |-- framework.properties
    |-- log4j.properties
    |-- profile
    `-- project.properties
    $RDECK_BASE/server/config
    |-- jaas-loginmodule.conf
    |-- realm.properties
    `-- rundeck-config.properties

## Configuration files
Configuration is specified in a number of standard Rundeck
configuration files generated during the installation process.

See the [Configuration layout](configuration.html#configuration-layout) section for where these
files reside for RPM and Launcher installations.

The purpose of each configuration file is described in its own section.

### admin.aclpolicy

Administrator access control policy defined with a "aclpolicy(5)" XML
document.

This file governs the access for the "admin" group and role. 

See [Authorization](../manual/getting-started.html#authorization) for information about setting up
policy files for other user groups.

### framework.properties

Configuration file used by shell tools and core Rundeck services. This file will be created for you at install time.

Some important settings:

* `framework.server.hostname`: Hostname of the Rundeck server node
* `framework.server.name`: Name (identity) of the Rundeck server node
* `framework.projects.dir`: Path to the directory containing Rundeck Project directories.  Default is `$RDECK_BASE/projects`.
* `framework.var.dir`: Base directory for output and temp files used by the server and CLI tools. Default is `$RDECK_BASE/var`.
* `framework.logs.dir`: Directory for log files written by core services and Rundeck Server's Job executions. Default is `$RDECK_BASE/var/logs`
* `framework.server.username`: Username for connection to the Rundeck server
* `framework.server.password`: Password for connection to the Rundeck server
* `framework.rundeck.url`: Base URL for Rundeck server.

Resource Provider settings:

* `framework.resources.allowedURL.X`: a sequence of regular expressions (for `X` starting at 0 and increasing). These are matched against requested providerURL values when
the `/project/name/resources/refresh` API endpoint is called. See [Refreshing Resources for a Project](../api/index.html#refreshing-resources-for-a-project).

SSH Connection settings:

* `framework.ssh.keypath`: Path to the SSH private key file used for SSH connections
* `framework.ssh.user`: Default username for SSH Connections, if not overridden by Node specific value.
* `framework.ssh.timeout`: timeout in milliseconds for SSH connections and executions. The default is "0" (no timeout).  You can modify this to change the maximum time allowed for SSH connections.

Other settings:

* `framework.log.dispatch.console.format`: Default format for non-terse node execution logging run by the `dispatch` CLI tool.

### log4j.properties

Rundeck uses [log4j] as its application logging facility. This file
defines the logging configuration for the Rundeck server. 

[log4j]: http://logging.apache.org/log4j/

### profile

Shell environment variables used by the shell tools. This file
contains several parameters needed during the startup of the shell
tools like umask, Java home and classpath, and SSL options.

### project.properties

Rundeck [project](../manual/getting-started.html#project) configuration file. One of these is
generated at project setup time. 

Property                          Description
----------                        -------------
`project.resources.file`          A local file path to read a resource model          document
`project.resources.url`           The URL to an external [Resource Model Source](node-resource-sources.html#resource-model-source).(Optional) 
`project.resources.allowedURL.X`  A sequence of regular expressions (for `X` starting at 0 and increasing). 
`resources.source.N...`               Defines a Resource model source see [Resource Model Sources](../manual/plugins.html#resource-model-sources).
----------------------------------

The `project.resources.allowedURL.X` values are matched against requested providerURL values when
the `/project/name/resources/refresh` API endpoint is called. See [Refreshing Resources for a Project](../api/index.html#refreshing-resources-for-a-project).

### jaas-loginmodule.conf

[JAAS] configuration for the Rundeck server. The listing below
shows the file content for a normal RPM installation. One can see it
specifies the use of the [PropertyFileLoginModule]:

    RDpropertyfilelogin {
      org.mortbay.jetty.plus.jaas.spi.PropertyFileLoginModule required
      debug="true"
      file="/etc/rundeck/realm.properties";
    };

[JAAS]: http://docs.codehaus.org/display/JETTY/JAAS
[PropertyFileLoginModule]: http://jetty.codehaus.org/jetty/jetty-6/apidocs/org/mortbay/jetty/plus/jaas/spi/PropertyFileLoginModule.html

### realm.properties

Property file user directory when PropertyFileLoginModule is
used. Specified from [jaas-loginmodule.conf](configuration.html#jaas-loginmodule.conf).

### rundeck-config.properties

The primary Rundeck webapp configuration file. Defines default
loglevel, datasource configuration, and
[GUI customization](gui-customization.html).

#### Notification email settings

The URL and From: address used in email notifications are managed via the settings located in the rundeck-config.properties file.

The two properties are:

* grails.serverURL
* grails.mail.default.from

Here's an example:

    grails.serverURL=https://node.fully.qualified.domain.name:4443
    grails.mail.default.from=deployer@domain.com

## GUI Admin Page

The Rundeck GUI has an Admin Page which contains lets you view and manage some configuration options.  If you have `admin` role access, when you log in you will see an "Admin" link in the header of the page near your username:

![Admin page link](../figures/fig0701.png)

Clicking on this link will take you to the Admin Page:

![Admin page](../figures/fig0702.png)

This page contains links to two sub-pages, and configuration information about the currently selected Project.

### System Information Page

The System Information page gives you a breakdown of some of the Rundeck server's system statistics and information:

![System Info Page](../figures/fig0703.png)

This information is also available via the API: [API > System Info](../api/index.html#system-info)

### User Profiles Page

The User Profiles page lists all User Profile records in the system. User Profiles are used to store some user preferences, and can be used to generate API Tokens for admin users.

![User Profiles Page](../figures/fig0704.png)

### Project Configuration

The selected project will be displayed with basic configuration options, and the list of configure Resource Model Sources, as well as the default Node Executor and File Copier settings.

If you click on "Configure Project", you will be taken to the Project Configuration form.

![Project Configuration Form](../figures/fig0705.png)

The first two fields allow configuration of some simple project basics.

First, you can enter a URL for a Resource Model Source, which will be used as a URL Resource Model Source with default configuration options.

Secondly, you can enter the Default SSH Key File, which is the private SSH Key file used
by default for SSH and SCP actions.  If you are not using SSH or SCP you do not have to enter one.

There are then several more sections: Resource Model Sources, Default Node Executor, and Default File Copier sections. These are described below:

### Resource Model Sources Configuration

This section lets you add and modify [Resource Model Sources](../manual/plugins.html#resource-model-sources) for the project.

To add a new one, click "Add Source". You are prompted to select a type of source. The list shown will include all of the built-in types of sources, as well as any Plugins you have installed.

![Add Resource Model Source](../figures/fig0706.png)

When you click "Add" for a type, you will be shown the configuration options for the type. 

![Configure Resource Model Source](../figures/fig0707.png)

You can then click "Cancel" or "Save" to discard or add the configuration to the list. 

Each item you add will be shown in the list:

![Configured Source](../figures/fig0708.png)

To edit an item in the list click the "Edit" button.  To delete an item in the list click the "Delete" button.

Each type of Resource Model Source will have different configuration settings of its own. The built-in Resource Model Source providers are shown below.

You can install more sources as plugins, see [Resource Model Source Plugins](../manual/plugins.html#resource-model-source-plugins).

#### File Resource Model Source

This is the File Resource Model Source configuration form:

![File Resource Model Source](../figures/fig0707.png)

See [File Resource Model Source Configuration](../manual/plugins.html#file-resource-model-source-configuration) for more configuration information.

#### Directory Resource Model Source

Allows a directory to be scanned for resource document files. All files
with an extension supported by one of the [Resource Model Document Formats](../manual/rundeck-basics.html#resource-model-document-formats) are included.

![Directory Resource Model Source](../figures/fig0709.png)

See [Directory Resource Model Source Configuration](../manual/plugins.html#directory-resource-model-source-configuration) for more configuration information.

#### Script Resource Model Source

This source can run an external script to produce the resource model 
definitions.

![Script Resource Model Source](../figures/fig0710.png)

See [Script Resource Model Source Configuration](../manual/plugins.html#script-resource-model-source-configuration) for more configuration information.

#### URL Resource Model Source

This source performs a HTTP GET request on a URL to return the 
resource definitions.

![URL Resource Model Source](../figures/fig0711.png)

See [URL Resource Model Source Configuration](../manual/plugins.html#url-resource-model-source-configuration) for more configuration information.

### Default Node Executor Configuration

When Rundeck executes a command on a node, it does so via a "Node Executor".
The most common built-in Node Executor is the "SSH" implementation, which uses
SSH to connect to the remote node, however other implementations can be used.

Select the Default Node Executor you wish to use for all remote Nodes for the project:

![Default Node Executor Choice](../figures/fig0712.png)

You can install more types of Node Executors as plugins, see [Node Execution Plugins](../manual/plugins.html#node-execution-plugins).

### Default File Copier Configuration

When Rundeck executes a script on a node, it does so by first copying the script as a file to the node, via a "File Copier". (It then uses a "Node Executor" to execute the script like a command.)

The most common built-in File Copier is the "SCP" implementation, which uses
SCP to copy the file to the remote node, however other implementations can be used.

Select the Default File Copier you wish to use for all remote Nodes for the project:

![Default File Copier Choice](../figures/fig0713.png)

You can install more types of File Copiers as plugins, see [Node Execution Plugins](../manual/plugins.html#node-execution-plugins).
