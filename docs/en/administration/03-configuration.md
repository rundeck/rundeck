% Configuration


# Configuration layout

Configuration file layout differs between the RPM and Launcher
installation methods.

## RPM layout

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
    /var/lib/rundeck/exp/webapp/WEB-INF/web.xml

## Launcher layout

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

# Configuration files
Configuration is specified in a number of standard Rundeck
configuration files generated during the installation process.

The purpose of each configuration file is described in its own section.

## admin.aclpolicy

Administrator access control policy defined with a [aclpolicy]
document.

This file governs the access for the "admin" group and role.

See [role based access control](access-control-policy.html) for information about setting up policy files for other user groups.

## framework.properties

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


SSH Connection settings (See [Plugins User Guide > SSH Plugins](../plugins-user-guide/ssh-plugins.html)):

* `framework.ssh.keypath`: Path to the SSH private key file used for SSH connections
* `framework.ssh.user`: Default username for SSH Connections, if not overridden by Node specific value.
* `framework.ssh-connect-timeout`: timeout in milliseconds for SSH connections. The default is "0" (no timeout).  You can modify this to change the connect/socket timeout. (Deprecated: `framework.ssh.timeout`.)
* `framework.ssh-command-timeout`: timeout in milliseconds for SSH commands. The default is "0" (no timeout).  You can modify this to change the maximum time allowed for SSH commands to run.

Other settings:

* `framework.log.dispatch.console.format`: Default format for non-terse node execution logging run by the `dispatch` CLI tool.
* `execution.script.tokenexpansion.enabled`: Whether inline script token expansion is enabled, default `true`.  If `false`, the "Inline Script Content" syntax described in [User Guide > Jobs > Context Variable Usage](../manual/jobs.html#context-variable-usage) is disabled.

Static authentication tokens for API access:

You can define the location of a .properties file in framework.properties:

* `rundeck.tokens.file=/etc/rundeck/tokens.properties`

The `tokens.properties` file should contain static authentication tokens you wish to use, keyed by the associated username:

    username: token_string
    username2: token_string2
    ...

The token_strings can be used as Authentication tokens to the [API](../api/index.html#token-authentication).

### Global execution variables

Entries in `framework.properties` in the form `framework.globals.X=Y` Adds a variable `X` available in all execution contexts as `${globals.X}`.

Global variables can be overridden in the [`project.properties`](#project.properties) by adding a line in the form of `project.globals.X=Y` and then accessing it as `${globals.X}`.


## log4j.properties

Rundeck uses [log4j] as its application logging facility. This file
defines the logging configuration for the Rundeck server.

[log4j]: https://logging.apache.org/log4j/2.x/

## cli-log4j.properties

This file defines the logging configuration for the Commandline tools.

## profile

Shell environment variables used by the shell tools. This file
contains several parameters needed during the startup of the shell
tools like umask, Java home and classpath, and SSL options.

## project.properties

Rundeck project configuration file when using Filesystem based project definitions (see [Project Setup - Project Definitions](project-setup.html#project-definitions)).

One of these is
generated at project setup time. Each project has a directory within the Rundeck projects directory, and the config file is within the `etc` subdirectory:

    $RDECK_BASE/projects/[PROJECT-NAME]/etc/project.properties

Property                                  Description
----------                                -------------
`project.name`                            Declare the project name.
`project.ssh-authentication`              SSH authentication type (eg, privateKey).
`project.ssh-keypath`                     SSH identify file.
`service.FileCopier.default.provider`     Default script file copier plugin.
`service.NodeExecutor.default.provider`   Default node executor plugin.
`resources.source.N...`                   Defines a Resource model source see [Resource Model Sources].
`project.globals.X`                       [Defines a Project Global variable](#project-global-execution-variables)
----------------------------------

Here's an example that configures a File source:

~~~~~~~~~~
resources.source.1.config.file=/var/rundeck/projects/${project.name}/etc/resources.xml
resources.source.1.config.generateFileAutomatically=true
resources.source.1.config.includeServerNode=true
resources.source.1.type=file
~~~~~~~~~~~

Another that configures a URL source:

~~~~~~~~
resources.source.2.config.cache=true
resources.source.2.config.timeout=30
resources.source.2.config.url=http\://example.com/nodes
resources.source.2.type=url
~~~~~~~~~

And one that configures a Directory source:

~~~~~~~~~~
resources.source.3.config.directory=/var/rundeck/projects/${project.name}/site_nodes
resources.source.3.type=directory
~~~~~~~~~~~~

Additional sources increment the source number. You can reference the project name by using the `${project.name}` context variable.

### Project Global execution variables

Project configuration entries of the form `project.globals.X=Y` Adds a variable `X` available in all execution contexts as `${globals.X}`, and overrides
any global with the same name defined in [`framework.properties`](#framework.properties).

## jaas-loginmodule.conf

[JAAS] configuration for the Rundeck server. The listing below
shows the file content for a normal RPM installation. One can see it
specifies the use of the PropertyFileLoginModule:

    RDpropertyfilelogin {
      org.eclipse.jetty.plus.jaas.spi.PropertyFileLoginModule required
      debug="true"
      file="/etc/rundeck/realm.properties";
    };

[JAAS]: https://wiki.eclipse.org/Jetty/Feature/JAAS

## realm.properties

Property file user directory when PropertyFileLoginModule is
used. Specified from [jaas-loginmodule.conf](#jaas-loginmodule.conf).

## Session timeout

Edit the web.xml to modify session-timeout from 30 to 90 minutes:

RPM: /var/lib/rundeck/exp/webapp/WEB-INF/web.xml

Example: Set the timeout to 60 minutes:

~~~~
diff /var/lib/rundeck/exp/webapp/WEB-INF/web.xml web.xml

214c214

< <session-timeout>30</session-timeout>

---

> <session-timeout>90</session-timeout>
~~~~

## rundeck-config.properties

This is the primary Rundeck webapp configuration file. Defines default
loglevel, datasource configuration, and
[GUI customization](gui-customization.html).

The following sections describe configuration values for this file.

### Security

* `rundeck.security.useHMacRequestTokens` : `true/false`.  Default: `true`.
   Switches between HMac based request tokens, and the default grails UUID
   tokens.  HMac tokens have a timeout, which may cause submitted forms or
   actions to fail with a message like "Token has expired".  
   If set to false, UUIDs will be used instead of HMac tokens,
   and they have no timeouts.
   The default timeout for tokens can be changed with the
   `-Dorg.rundeck.web.infosec.HMacSynchronizerTokensHolder.DEFAULT_DURATION=[timeout in ms]`.

* `rundeck.security.apiCookieAccess.enabled`: `true/false`. Default: `true`.  
    Determines whether access to the API is allowed if the API client
    authenticates via session cookies (i.e. username and password login.)  If
    set to `false`, the current CLI tools and API libraries will not operate
    correctly if they use username and password login.

* `rundeck.api.tokens.duration.max`: Duration string indicating maximum lifetime of API Tokens. If unset, the value
    will be "30d" (30 days). Format: "##{ydhms}" (years, days, hours, minutes, seconds). 
    If you want to disable the max expiration you can set it to 0 and create token with 0 duration that don't expire.

* `rundeck.security.csrf.referer.filterMethod`:`NONE|POST|*`. Set HTTP Method to filter based on Referer header.  Can be POST, or "*" for all methods. Default: NONE (disabled)

* `rundeck.security.csrf.referer.allowApi`: `true|false`. Allow /api/* requests without requiring matching Referer header. Default: true.

* `rundeck.security.csrf.referer.requireHttps`: `true|false`. If server URL is HTTPS, Require referer header to be from HTTPS version of server URL, if false allow HTTP as well. Default: true.
 
### Execution Mode

* `rundeck.executionMode`:`active/passive`. Default `active`. Set the Execution
  Mode for the Rundeck server.

Rundeck can be in `active` or `passive` execution mode.

* `active` mode: Jobs, scheduled Jobs, and adhoc executions can be run.
* `passive` mode: No Jobs or adhoc executions can be run.

Setting Rundeck to `passive` mode prevents users from running anything on the
system and is useful when managing Rundeck server clusters.

### Project Configuration Storage settings

The [Project Setup - Project Definitions](project-setup.html#project-definitions) mechanism is configured within this file, see:

* [Project Storage][]

[Project Storage]: storage-facility.html#project-storage

### Key Storage settings

The [Key storage](key-storage.html) mechanism is configured within this file, see:

* [Configuring Storage Plugins][]
* [Configuring Storage Converter Plugins][]

[Configuring Storage Plugins]: ../plugins-user-guide/configuring.html#storage-plugins
[Configuring Storage Converter Plugins]: ../plugins-user-guide/configuring.html#storage-converter-plugins

### Notification email settings

See [Email Settings: Notification email settings](email-settings.html#notification-email-settings)

### Custom Email Templates

See [Email Settings: Custom Email Templates](email-settings.html#custom-email-templates)

### Execution finalize retry settings

If a sporadic DB connection failure happens when an execution finishes, Rundeck may fail to update the state of the execution in the database, causing the execution to appear is if it is still "running".

Rundeck now attempts to retry the update to correctly register the final state of the execution.  You can tune how many times and how often this retry occurs with these config values:

    # attempt to retry the final state update
    rundeck.execution.finalize.retryMax=10
    rundeck.execution.finalize.retryDelay=5000

    # attempt to retry updating job statistics after execution finishes
    rundeck.execution.stats.retryMax=3
    rundeck.execution.stats.retryDelay=5000

Delay is in milliseconds. If a max is set to `-1`, then retries will happen indefinitely.

### Metrics servlets

Rundeck includes the [Metrics](http://metrics.dropwizard.io/3.0.2/) servlets.  You can selectively disable these by setting these config values:

`rundeck.web.metrics.servlets.[name].enabled=true/false`

Servlet names are:

* `metrics`
* `threads`
* `ping`
* `healthcheck`

All of the servlets are enabled by default.

[Resource Model Sources]: ../administration/managing-node-sources.html

### Pagination defaults

Default paging size for the Activity page and results from execution API queries can be changed.

    rundeck.pagination.default.max=20

### Job Remote Option URL connection parameters

Change the defaults for for [Job Remote Option Value URLs](../manual/jobs.html#remote-option-values) loading.

**Socket read timeout**

Max wait time reading from socket.

Default value: `10` (seconds)

Change this by setting:

    rundeck.jobs.options.remoteUrlTimeout=[seconds]

**Connection timeout**

Max wait time attempting to make the connection.

Default value: (no timeout)

Change this by setting:

    rundeck.jobs.options.remoteUrlConnectionTimeout=[seconds]

**No response retry**

If the request is sent, but the server disconnects without a response (e.g. server is overloaded), retry the request this many times.

Default value: 3

Change this by setting:

    rundeck.jobs.options.remoteUrlRetry=[total]

### Job File Option Uploads

Values to configure file uploads for File type Job options:

Max temp file size.
File size in bytes or with a suffix of `k`,`m`,`g`,`t` (kilo,mega,giga,tera).

    rundeck.fileUploadService.tempfile.maxsize=200M

Max temp file expiration (duration in milliseconds).
The uploaded file will be removed if not used as a job option within ths time period.
(This primarily affects Job executions performed via API
because the File Upload and Job Run are performed as separate steps.)

    # default is 10 minutes
    rundeck.fileUploadService.tempfile.expiration=600000


### Groovy config format

You can change you rundeck-config.properties to a rundeck-config.groovy. 

The groovy format is a java-like language, and it is not the same as properties.

Make sure you put quotes around all string values, but it is not necessary for true/false or numbers.

java properties format:

~~~ {.properties}
some.property=value 
~~~

groovy format:

~~~ {.groovy}
some.property="value"
~~~

You can also use nested values using curly brackets, or use dot-notation "a.b.c",
but since it is not simple text properties, strings have to be quoted.

E.g. : a.b.c="blah" is the same as:

~~~ {.groovy}
a{ 
    b{
        c="blah"
    }
}
~~~

### Specify config file location

You will need to point rundeck at the new filename when you start up rundeck:

* Launcher:

        java -jar -Drundeck.config.name=rundeck-config.groovy rundeck-launcher.jar

RPM: Add this to the `/etc/sysconfig/rundeckd` file:

        export RDECK_CONFIG_FILE="/etc/rundeck/rundeck-config.groovy"

RPM/DEB: Add this to the `/etc/default/rundeckd` file:

        export RDECK_CONFIG_FILE="/etc/rundeck/rundeck-config.groovy"
