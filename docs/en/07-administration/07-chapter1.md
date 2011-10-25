# Administration

## Startup and shutdown

RunDeck installation includes a control script used for starting and
stopping the RunDeck server process.
The control script provides a number of actions:

    rundeckd [start|stop|restart|condrestart|status]

### RPM

The RPM installation includes the placement of the boot control script
that will automatically start RunDeck when the system boots.

The script is located here: `/etc/init.d/rundeckd` 

*Startup*

    /etc/init.d/rundeckd start

*Shutdown*

    /etc/initd./rundeckd stop
    
#### Setting JAVA_HOME

When using the RPM, by default rundeck will use _java_ found in your path.  Various RPM based 
distributions provide ways of managing which version of java is found.  CentOS uses 
_/usr/sbin/alternatives_ and the processing of setting alternatives can be found here: 
[http://wiki.centos.org/HowTos/JavaOnCentOS](http://wiki.centos.org/HowTos/JavaOnCentO).

If you have installed a JDK or JRE in a unique directory and do not want to alter the global system
configuration, then simply setting JAVA_HOME before running any command will use the version of java
found in JAVA_HOME/bin.  Updating /etc/rundeck/profile with JAVA_HOME is another option as 
well.
    
### Launcher

The Launcher installation generates the script into the RDECK_BASE directory.

The script is located here: `$RDECK_BASE/server/sbin/rundeckd`.

*Startup*

    $RDECK_BASE/server/sbin/rundeckd start

*Shutdown*

    $RDECK_BASE/server/sbin/rundeckd stop

You may choose to incorporate this script into your server's operating
system specific boot process.

## Configuration

### Configuration layout

Configuration file layout differs between the RPM and Launcher
installation methods. See [RPM layout](#rpm-layout) and
[Launcher layout](#launcher-layout) for details.

#### RPM layout

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

#### Launcher layout

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

### Configuration files
Configuration is specified in a number of standard RunDeck
configuration files generated during the installation process.

See the [Configuration layout](#configuration-layout) section for where these
files reside for RPM and Launcher installations.

The purpose of each configuration file is described in its own section.

#### admin.aclpolicy

Administrator access control policy defined with a "aclpolicy(5)" XML
document.

This file governs the access for the "admin" group and role. 

See [Authorization](#authorization) for information about setting up
policy files for other user groups.

#### framework.properties

Configuration file used by shell tools and core RunDeck services. This file will be created for you at install time.

Some important settings:

* `framework.node.hostname`: Hostname of the RunDeck server node
* `framework.node.name`: Name (identity) of the RunDeck server node
* `framework.projects.dir`: Path to the directory containing RunDeck Project directories.  Default is `$RDECK_BASE/projects`.
* `framework.var.dir`: Base directory for output and temp files used by the server and CLI tools. Default is `$RDECK_BASE/var`.
* `framework.logs.dir`: Directory for log files written by core services and RunDeck Server's Job executions. Default is `$RDECK_BASE/var/logs`
* `framework.server.username`: Username for connection to the RunDeck server
* `framework.server.password`: Password for connection to the RunDeck server
* `framework.rundeck.url`: Base URL for RunDeck server.

Resource Provider settings:

* `framework.resources.allowedURL.X`: a sequence of regular expressions (for `X` starting at 0 and increasing). These are matched against requested providerURL values when
the `/project/name/resources/refresh` API endpoint is called. See [Refreshing Resources for a Project](api/index.html#refreshing-resources-for-a-project).

SSH Connection settings:

* `framework.ssh.keypath`: Path to the SSH private key file used for SSH connections
* `framework.ssh.user`: Default username for SSH Connections, if not overridden by Node specific value.
* `framework.ssh.timeout`: timeout in milliseconds for SSH connections and executions. The default is "0" (no timeout).  You can modify this to change the maximum time allowed for SSH connections.

Other settings:

* `framework.log.dispatch.console.format`: Default format for non-terse node execution logging run by the `dispatch` CLI tool.

#### log4j.properties

RunDeck uses [log4j] as its application logging facility. This file
defines the logging configuration for the RunDeck server. 

[log4j]: http://logging.apache.org/log4j/

#### profile

Shell environment variables used by the shell tools. This file
contains several parameters needed during the startup of the shell
tools like umask, Java home and classpath, and SSL options.

#### project.properties

RunDeck [project](#project) configuration file. One of these is
generated at project setup time. 

Property                          Description
----------                        -------------
`project.resources.file`          A local file path to read a resource model          document
`project.resources.url`           The URL to an external [Resource Model Source](#resource-model-source).(Optional) 
`project.resources.allowedURL.X`  A sequence of regular expressions (for `X` starting at 0 and increasing). 
`resources.source.N...`               Defines a Resource model source see [Resource Model Sources](#resource-model-sources).
----------------------------------

The `project.resources.allowedURL.X` values are matched against requested providerURL values when
the `/project/name/resources/refresh` API endpoint is called. See [Refreshing Resources for a Project](api/index.html#refreshing-resources-for-a-project).

#### jaas-loginmodule.conf

[JAAS] configuration for the RunDeck server. The listing below
shows the file content for a normal RPM installation. One can see it
specifies the use of the [PropertyFileLoginModule]:

    RDpropertyfilelogin {
      org.mortbay.jetty.plus.jaas.spi.PropertyFileLoginModule required
      debug="true"
      file="/etc/rundeck/realm.properties";
    };

[JAAS]: http://docs.codehaus.org/display/JETTY/JAAS
[PropertyFileLoginModule]: http://jetty.codehaus.org/jetty/jetty-6/apidocs/org/mortbay/jetty/plus/jaas/spi/PropertyFileLoginModule.html

#### realm.properties

Property file user directory when PropertyFileLoginModule is
used. Specified from [jaas-loginmodule.conf](#jaas-loginmodule.conf).

#### rundeck-config.properties

The primary RunDeck webapp configuration file. Defines default
loglevel, datasource configuration, [role mapping](#role-mapping), and
[GUI customization](#customizing-rundeck-gui).

##### Notification email settings

The URL and From: address used in email notifications are managed via the settings located in the rundeck-config.properties file.

The two properties are:

* grails.serverURL
* grails.mail.default.from

Here's an example:

    grails.serverURL=https://node.fully.qualified.domain.name:4443
    grails.mail.default.from=deployer@domain.com

### GUI Admin Page

The RunDeck GUI has an Admin Page which contains lets you view and manage some configuration options.  If you have `admin` role access, when you log in you will see an "Admin" link in the header of the page near your username:

![Admin page link](figures/fig0701.png)

Clicking on this link will take you to the Admin Page:

![Admin page](figures/fig0702.png)

This page contains links to two sub-pages, and configuration information about the currently selected Project.

#### System Information Page

The System Information page gives you a breakdown of some of the RunDeck server's system statistics and information:

![System Info Page](figures/fig0703.png)

This information is also available via the API: [API > System Info](api/index.html#system-info)

#### User Profiles Page

The User Profiles page lists all User Profile records in the system. User Profiles are used to store some user preferences, and can be used to generate API Tokens for admin users.

![User Profiles Page](figures/fig0704.png)

#### Project Configuration

The selected project will be displayed with basic configuration options, and the list of configure Resource Model Sources, as well as the default Node Executor and File Copier settings.

If you click on "Configure Project", you will be taken to the Project Configuration form.

![Project Configuration Form](figures/fig0705.png)

The first two fields allow configuration of some simple project basics.

First, you can enter a URL for a Resource Model Source, which will be used as a URL Resource Model Source with default configuration options.

Secondly, you can enter the Default SSH Key File, which is the private SSH Key file used
by default for SSH and SCP actions.  If you are not using SSH or SCP you do not have to enter one.

There are then several more sections: Resource Model Sources, Default Node Executor, and Default File Copier sections. These are described below:

#### Resource Model Sources Configuration

This section lets you add and modify [Resource Model Sources](#resource-model-sources) for the project.

To add a new one, click "Add Source". You are prompted to select a type of source. The list shown will include all of the built-in types of sources, as well as any Plugins you have installed.

![Add Resource Model Source](figures/fig0706.png)

When you click "Add" for a type, you will be shown the configuration options for the type. 

![Configure Resource Model Source](figures/fig0707.png)

You can then click "Cancel" or "Save" to discard or add the configuration to the list. 

Each item you add will be shown in the list:

![Configured Source](figures/fig0708.png)

To edit an item in the list click the "Edit" button.  To delete an item in the list click the "Delete" button.

Each type of Resource Model Source will have different configuration settings of its own. The built-in Resource Model Source providers are shown below.

You can install more sources as plugins, see [Resource Model Source Plugins](#resource-model-source-plugins).

##### File Resource Model Source

This is the File Resource Model Source configuration form:

![File Resource Model Source](figures/fig0707.png)

See [File Resource Model Source Configuration](#file-resource-model-source-configuration) for more configuration information.

##### Directory Resource Model Source

Allows a directory to be scanned for resource document files. All files
with an extension supported by one of the [Resource Model Document Formats](#resource-model-document-formats) are included.

![Directory Resource Model Source](figures/fig0709.png)

See [Directory Resource Model Source Configuration](#directory-resource-model-source-configuration) for more configuration information.

##### Script Resource Model Source

This source can run an external script to produce the resource model 
definitions.

![Script Resource Model Source](figures/fig0710.png)

See [Script Resource Model Source Configuration](#script-resource-model-source-configuration) for more configuration information.

##### URL Resource Model Source

This source performs a HTTP GET request on a URL to return the 
resource definitions.

![URL Resource Model Source](figures/fig0711.png)

See [URL Resource Model Source Configuration](#url-resource-model-source-configuration) for more configuration information.

#### Default Node Executor Configuration

When RunDeck executes a command on a node, it does so via a "Node Executor".
The most common built-in Node Executor is the "SSH" implementation, which uses
SSH to connect to the remote node, however other implementations can be used.

Select the Default Node Executor you wish to use for all remote Nodes for the project:

![Default Node Executor Choice](figures/fig0712.png)

You can install more types of Node Executors as plugins, see [Node Execution Plugins](#node-execution-plugins).

#### Default File Copier Configuration

When RunDeck executes a script on a node, it does so by first copying the script as a file to the node, via a "File Copier". (It then uses a "Node Executor" to execute the script like a command.)

The most common built-in File Copier is the "SCP" implementation, which uses
SCP to copy the file to the remote node, however other implementations can be used.

Select the Default File Copier you wish to use for all remote Nodes for the project:

![Default File Copier Choice](figures/fig0713.png)

You can install more types of File Copiers as plugins, see [Node Execution Plugins](#node-execution-plugins).

## Logs

Depending on the installer used, the log files will be under a base
directory:

*   RPM: `/var/log/rundeck`
*   Launcher: `$RDECK_BASE/server/logs`

The following files will be found in the log directory:

     .
     |-- command.log
     |-- rundeck.audit.log
     |-- rundeck.jobs.log
     |-- rundeck.options.log
     |-- rundeck.log
     `-- service.log

Different facilities log to their own files:

* `command.log`: Shell tools log their activity to the command.log
* `rundeck.audit.log`: Authorization messages pertaining to aclpolicy
* `rundeck.job.log`: Log of all job definition changes
* `rundeck.options.log`: Logs remote HTTP requests for Options JSON data
* `rundeck.log`: General RunDeck application messages
* `service.log`: Standard input and output generated during runtime

See the [#log4j.properties](#log4j.properties) section for information 
about customizing log message formats and location.

## Backup and recovery

While running, export the Job definitions if you do not have these in source control:

(1) Export the jobs. You will have to do this for each project

        rd-jobs list -f /path/to/backup/dir/project1/jobs.xml -p project1
        rd-jobs list -f /path/to/backup/dir/project2/jobs.xml -p project2
        ...

(2) Stop the server. See: [startup and shutdown](#startup-and-shtudown). (RunDeck data file backup should only be done with the server down.)

        rundeckd stop

(3) Copy the data files. (Assumes file datastore configuration). The
location of the data directory depends on the installation method:

       * RPM install: `/var/lib/rundeck/data`
       * Launcher install: `$RDECK_BASE/server/data`

            cp -r data /path/to/backup/dir
             
(3) Copy the log (execution output) files.

       * RPM install: `/var/lib/rundeck/logs`
       * Launcher install: `$RDECK_BASE/var/logs`

            cp -r logs /path/to/backup/dir

(4) Start the server

         rundeckd start

### Recovery

(1) Stop the server. See: [startup and shutdown](#startup-and-shtudown). (RunDeck recovery should only be done with the server down.)

        rundeckd stop

(2) Restore data/logs dir from backup (Refer to above for appropriate log/data path):

        cp -r /path/to/backup/logs logspath
        cp -r /path/to/backup/data datapath

(3) Start the server:

        rundeckd start

(4) Reload the Job definitions. You will have to do this for each project:

        rd-jobs load -f /path/to/backup/dir/project1/jobs.xml -p project1
        rd-jobs load -f /path/to/backup/dir/project2/jobs.xml -p project2

## Relational Database

You can configure RunDeck to use a RDB instead of the default file-based data storage.

You must modify the `server/config/rundeck-config.properties` file, to change the `dataSource` configuration, and you will have to add the appropriate JDBC driver JAR file to the lib directory.

### Enable rdbsupport

First, you **must** enable the `rundeck.v14.rdbsupport` property:

    #note, make sure this is set to "true" if you are using Oracle or Mysql
    rundeck.v14.rdbsupport=true

This makes RunDeck use table/field names that are compatible with Oracle/Mysql.

Note: It is safe to set this to true if you are using the default file based backend, but only for a fresh install. It will cause a problem if you set it to true for an existing Rundeck 1.3 HSQLDB database.  Make sure it is set to "false" or is absent from your config file if you are upgrading from Rundeck 1.3 and using the filesystem storage.

### Customize the Datasource

The default dataSource is configured for filesystem storage using HSQLDB:

    dataSource.url = jdbc:hsqldb:file:/var/lib/rundeck/data/grailsdb;shutdown=true

Here is an example configuration to use an Oracle backend:

    dataSource.url = jdbc:oracle:thin:@localhost:1521:XE
    dataSource.driverClassName = oracle.jdbc.driver.OracleDriver
    dataSource.username = dbuser
    dataSource.password = dbpass
    dataSource.dialect = org.hibernate.dialect.Oracle10gDialect

Here is an example configuration to use Mysql:

    dataSource.url = jdbc:mysql://myserver/rundeckdb
    dataSource.username = dbuser
    dataSource.password = dbpass

### Add the JDBC Driver

Copy the appropriate JDBC driver, such as "ojdbc14.jar" for Oracle into the server `lib` dir:

    cp ojdbc14.jar $RDECK_BASE/server/lib

Or:

    cp mysql-connector-java-5.1.17-bin.jar $RDECK_BASE/server/lib

## SSH

RunDeck uses [SSH] for remote execution. 
You do _not_ need to have root account access on either the server or
the remote hosts.  

[SSH]: http://en.wikipedia.org/wiki/Secure_Shell

### SSH configuration requirements

* The SSH configuration requires that the RunDeck server machine can
  ssh commands to the client machines. 
* SSH is assumed to be installed and configured appropriately to allow
  this access.   
* No passphrase should be set.
* SSH should not prompt for a password. There are many resources
available on how to configure ssh to use public key authentication
instead of passwords such as:
[Password-less logins with OpenSSH](http://www.debian-administration.org/articles/152)
or
[How-To: Password-less SSH](http://www.cs.wustl.edu/~mdeters/how-to/ssh/)

### SSH key generation

* The RunDeck installation can be configured to use RSA _or_ DSA
  type keys.
  
Here's an example of SSH RSA key generation on a Linux system:

    $ ssh-keygen -t rsa
    Generating public/private rsa key pair.
    Enter file in which to save the key (/home/demo/.ssh/id_rsa): 
    Enter passphrase (empty for no passphrase): 
    Enter same passphrase again: 
    Your identification has been saved in /home/demo/.ssh/id_rsa.
    Your public key has been saved in /home/demo/.ssh/id_rsa.pub.
    The key fingerprint is:
    a7:31:01:ca:f0:62:42:9d:ab:c8:b7:9c:d1:80:76:c6 demo@ubuntu
    The key's randomart image is:
    +--[ RSA 2048]----+
    | .o . .          |
    |.  * . .         |
    |. = =   .        |
    | = E     .       |
    |+ + o   S .      |
    |.o o .   =       |
    |  o +   .        |
    |   +             |
    |                 |
    +-----------------+

### Configuring remote machine for SSH 

To be able to directly ssh to remote machines, the SSH public key of
the client should be shared to the remote machine.
  
Follow the steps given below to enable ssh to remote machines.

The ssh public key should be copied to the `authorized_keys` file of
the remote machine. The public key will be available in
`~/.ssh/id_rsa.pub` file.
  
The `authorized_keys` file should be created in the `.ssh` directory of
the remote machine.
  
The file permission of the authorized key should be read/write for
the user and nothing for group and others. To do this check the
permission and change it as shown below.

    $ cd ~/.ssh
    $ ls -la
    -rw-r--r--   1 raj  staff     0 Nov 22 18:14 authorized_keys

    $ chmod 600 authorized_keys 
    $ ls -la
    -rw-------   1 raj  staff     0 Nov 22 18:14 authorized_keys

The permission for the .ssh directory of the remote machine should
be read/write/execute for the user and nothing for the group and
others. To do this, check the permission and change it as shown
below.  

    $ ls -la
    drwxr-xr-x   2 raj  staff    68 Nov 22 18:19 .ssh
    $ chmod 700 .ssh
    $ ls -la
    drwx------   2 raj  staff    68 Nov 22 18:19 .ssh

If you are running RunDeck on Windows, we heartily recommend using
[Cygwin] on Windows as it includes SSH and a number of
Unix-like tools that are useful when you work in a command line
environment.

[Cygwin]: http://www.cygwin.org

### Configuring SSH private keys

The built-in SSH connector allows the private key to be specified in several different ways.  You can configure it per-node, per-project, or per-RunDeck instance.

When connecting to the remote node, RunDeck will look for a property/attribute specifying the location of the private key file, in this order, with the first match having precedence:

1. **Node level**: `ssh-keypath` attribute on the Node. Applies only to the target node.
2. **Project level**: `project.ssh-keypath` property in `project.properties`.  Applies to any project node by default.
3. **RunDeck level**: `framework.ssh-keypath` property in `framework.properties`. Applies to all projects by default.
4. **RunDeck level**:  `framework.ssh.keypath` property in `framework.properties`. Applies to all projects by default (included for compatibility with Rundeck < 1.3). (default value: `~/.ssh/id_rsa`).

### Passing environment variables through remote command

To pass environment variables through remote command
dispatches, it is required to properly configure the SSH server on the
remote end. See the `AcceptEnv` directive in the "sshd\_config(5)"
manual page for instructions. 

Use a wild card pattern to permit `RD_` prefixed variables to provide
open access to RunDeck generated environment variables.

## Managing logins

### realm.properties

These instructions explain how to manage user credentials for 
RunDeck in the <code>realm.properties</code> file.

The default RunDeck webapp handles user authentication via its
container, which in turn is configured to pull its user authentication
from the `$RDECK_BASE/server/config/realm.properties` file. 
This file is created at the time that you install the server.

Assuming it wasn't modified, your realm.properties file will
probably look something like this:

    #
    # This file defines users passwords and roles for a HashUserRealm
    #
    # The format is
    #  <username>: <password>[,<rolename> ...]
    #
    # Passwords may be clear text, obfuscated or checksummed.  
    #
    # This sets the default user accounts for the RunDeck apps
    #
    admin:admin,user,admin
    user:user,user

*Adding additional users*

You may wish to have additional users with various privileges rather
than giving out role accounts to groups.  You may also want to avoid
having the passwords in plaintext within the configuration file.  

To accomplish this, you'll need a properly hashed or encrypted
password to use in the config.  On the RunDeck server, move into
the directory that contains your installation and pass the
username and password to the `Password` utility.  In this example,
we'll setup a new user named "jsmith", with a password of "mypass":

    $ cd $RDECK_BASE
    $ java -cp server/lib/jetty-6.1.21.jar:server/lib/jetty-util-6.1.21.jar org.mortbay.jetty.security.Password jsmith mypass
    OBF:1xfd1zt11uha1ugg1zsp1xfp
    MD5:a029d0df84eb5549c641e04a9ef389e5
    CRYPT:jsnDAc2Xk4W4o

Then add this to the `realm.properties` file with a line like so:

    jsmith: MD5:a029d0df84eb5549c641e04a9ef389e5,user,admin

Then restart RunDeck to ensure it picks up the change and you're done.


### Active Directory

*note* Because the underlying security mechanism relies on JAAS, you are free to use what ever JAAS provider you feel is suitable for your environment.

(1)  Setup the LDAP login module configuration file

    Create a `jaas-activedirectory.conf` file in the same directory as the `jaas-loginmodule.conf` file.
    
    * RPM install: /etc/rundeck/
    * Launcher install: $RDECK_BASE/server/config
    
    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    activedirectory {
        com.dtolabs.rundeck.jetty.jaas.JettyCachingLdapLoginModule required
        debug="true"
        contextFactory="com.sun.jndi.ldap.LdapCtxFactory"
        providerUrl="ldap://localhost:389"
        bindDn="cn=Manager,dc=rundeck,dc=com"
        bindPassword="secret"
        authenticationMethod="simple"
        forceBindingLogin="true"
        userBaseDn="ou=users,dc=rundeck,dc=com"
        userRdnAttribute="cn"
        userIdAttribute="cn"
        userPasswordAttribute="unicodePwd"
        userObjectClass="user"
        roleBaseDn="ou=roles,dc=rundeck,dc=com"
        roleNameAttribute="cn"
        roleMemberAttribute="member"
        roleObjectClass="group"
        cacheDurationMillis="300000"
        reportStatistics="true";
        };
    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

**Note**: The `bindDn` and `bindPassword` must escape any special characters with `\` character. Special characters include `\` (backslash), as well as `!` (exclamation).

(2) To override the default JAAS configuration file, you will need to supply the RunDeck server with the proper path to the new one, and a `loginmodule.name` Java system property to identify the new login module by name.

    The JAAS configuration file location is specified differently between the Launcher and the RPM.
    
    **For the Launcher**:  the `loginmodule.conf.name` Java system property is used to identify the *name* of the config file, which must be located in the `$RDECK_BASE/server/config` dir.
    
    You can simply specify the system properties on the java commandline:
    
        java -Dloginmodule.conf.name=jaas-activedirectory.conf \
            -Dloginmodule.name=activedirectory \
            -jar rundeck-launcher-x.x.jar
            
    Otherwise, if you are starting the Launcher via the supplied `rundeckd` script, you can modify the `RDECK_JVM` value in the `$RDECK_BASE/etc/profile` file to add two JVM arguments:
    
        export RDECK_JVM="-Dloginmodule.conf.name=jaas-activedirectory.conf \
            -Dloginmodule.name=activedirectory"
    
    Note: more information about using the launcher and useful properties are under [Getting Started - Launcher Options](#launcher-options).
    
    **For the RPM installation**: the absolute path to the JAAS config file must be specified with the `java.security.auth.login.config` property.
    
    Update the `RDECK_JVM` in `/etc/rundeck/profile` by changing the following two JVM arguments:
    
        export RDECK_JVM="-Djava.security.auth.login.config=/etc/rundeck/jaas-loginmodule.conf \
               -Dloginmodule.name=RDpropertyfilelogin \
           
    to
    
        export RDECK_JVM="-Djava.security.auth.login.config=/etc/rundeck/jaas-activedirectory.conf \
               -Dloginmodule.name=activedirectory \


(3) Restart rundeckd

    `sudo /etc/init.d/rundeckd restart`

(4) Attempt to logon

    If everything was configured correctly, you will be able to access RunDeck using your AD credentials.  If something did not go smoothly, look at `/var/log/rundeck/service.log` for stack traces that may indicate what is wrong.

#### Communicating over secure ldap (ldaps://)

The default port for communicating with active directory is 389, which is insecure.  The secure port is 686, but the LoginModule describe above requires that the AD certificate or organizations CA certificate be placed in a truststore.  The truststore provided with rundeck `/etc/rundeck/ssl/truststore` is used for the local communication between the cli tools and the rundeck server.

Before you can establish trust, you need to get the CA certificate.  Typically, this would require a request to the organization's security officer to have them send you the certificate.  It's also often found publicly if your organization does secure transactions.

Another option is to interrogate the secure ldap endpoint with openssl.  The example below shows a connection to paypal.com on port 443.  The first certificate is the machine and that last is the CA.  Pick the last certificate.  

*note* that for Active Directory, the host would be the Active Directory server and port 686.  
*note* Certificates are PEM encoded and start with -----BEGIN CERTIFICATE----- end with -----END CERTIFICATE----- inclusive.

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
$ openssl s_client -showcerts -connect paypal.com:443
CONNECTED(00000003)
depth=1 C = US, O = "VeriSign, Inc.", OU = VeriSign Trust Network, OU = Terms of use at https://www.verisign.com/rpa (c)09, CN = VeriSign Class 3 Secure Server CA - G2
verify error:num=20:unable to get local issuer certificate
verify return:0
---
Certificate chain
 0 s:/C=US/ST=California/L=San Jose/O=PayPal, Inc./OU=Information Systems/CN=paypal.com
   i:/C=US/O=VeriSign, Inc./OU=VeriSign Trust Network/OU=Terms of use at https://www.verisign.com/rpa (c)09/CN=VeriSign Class 3 Secure Server CA - G2
-----BEGIN CERTIFICATE-----
MIIFDjCCA/agAwIBAgIQL0NdM6l74HplIwrcygDcCTANBgkqhkiG9w0BAQUFADCB
tTELMAkGA1UEBhMCVVMxFzAVBgNVBAoTDlZlcmlTaWduLCBJbmMuMR8wHQYDVQQL
ExZWZXJpU2lnbiBUcnVzdCBOZXR3b3JrMTswOQYDVQQLEzJUZXJtcyBvZiB1c2Ug
YXQgaHR0cHM6Ly93d3cudmVyaXNpZ24uY29tL3JwYSAoYykwOTEvMC0GA1UEAxMm
VmVyaVNpZ24gQ2xhc3MgMyBTZWN1cmUgU2VydmVyIENBIC0gRzIwHhcNMTAwNTAz
MDAwMDAwWhcNMTIwNjExMjM1OTU5WjB/MQswCQYDVQQGEwJVUzETMBEGA1UECBMK
Q2FsaWZvcm5pYTERMA8GA1UEBxQIU2FuIEpvc2UxFTATBgNVBAoUDFBheVBhbCwg
SW5jLjEcMBoGA1UECxQTSW5mb3JtYXRpb24gU3lzdGVtczETMBEGA1UEAxQKcGF5
cGFsLmNvbTCBnzANBgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEArlvu+86iVb4RXdX+
8MjmGynNSl+Hu2/ZJ7nU1sj5O2jASWwFH7PFUv10qlRtL+gi3Rjw+zFN958iUetz
ef4CxQYf52PA7Uj9YlFEzLz7f8UDotu4WNLM3QGbLrqS28pPb2qKyyOQDvwNpI1c
Jt4JDa0ofVnCdICZEnf+cJB121MCAwEAAaOCAdEwggHNMAkGA1UdEwQCMAAwCwYD
VR0PBAQDAgWgMEUGA1UdHwQ+MDwwOqA4oDaGNGh0dHA6Ly9TVlJTZWN1cmUtRzIt
Y3JsLnZlcmlzaWduLmNvbS9TVlJTZWN1cmVHMi5jcmwwRAYDVR0gBD0wOzA5Bgtg
hkgBhvhFAQcXAzAqMCgGCCsGAQUFBwIBFhxodHRwczovL3d3dy52ZXJpc2lnbi5j
b20vcnBhMB0GA1UdJQQWMBQGCCsGAQUFBwMBBggrBgEFBQcDAjAfBgNVHSMEGDAW
gBSl7wsRzsBBA6NKZZBIshzgVy19RzB2BggrBgEFBQcBAQRqMGgwJAYIKwYBBQUH
MAGGGGh0dHA6Ly9vY3NwLnZlcmlzaWduLmNvbTBABggrBgEFBQcwAoY0aHR0cDov
L1NWUlNlY3VyZS1HMi1haWEudmVyaXNpZ24uY29tL1NWUlNlY3VyZUcyLmNlcjBu
BggrBgEFBQcBDARiMGChXqBcMFowWDBWFglpbWFnZS9naWYwITAfMAcGBSsOAwIa
BBRLa7kolgYMu9BSOJsprEsHiyEFGDAmFiRodHRwOi8vbG9nby52ZXJpc2lnbi5j
b20vdnNsb2dvMS5naWYwDQYJKoZIhvcNAQEFBQADggEBADbOGDkzZy22y+fW4OR7
wkx+1E3BxnRMZYx89OOykzTEUt2UV5DVuccUbqxTxg9/4pKMYJLywYn9UIOPHpwx
fbvMQNpdqV3JSuGMTwpROrMvC3bT13aCxxDnozeCjd/lH74m6G5ef2EUd3m5Y+iC
fMPo2NMrVyQYOCtpJurh9Tre1gQFHUYAXw8ty0YxfMoR/7FwYbd4spiZJwL2Mvfn
9gn24dWuKY7JaFutomwOM78rGzBDZZ/spEx9rcNa3OuVHcqBamnnXQZlZJilj4LE
buMBx8ti5Oqy4z1u1vzA8HalseiZerqFtBGOIakXdto8qLnwYEHQvVa/ih5iTsi3
Ja8=
-----END CERTIFICATE-----
 1 s:/C=US/O=VeriSign, Inc./OU=VeriSign Trust Network/OU=Terms of use at https://www.verisign.com/rpa (c)09/CN=VeriSign Class 3 Secure Server CA - G2
   i:/C=US/O=VeriSign, Inc./OU=Class 3 Public Primary Certification Authority - G2/OU=(c) 1998 VeriSign, Inc. - For authorized use only/OU=VeriSign Trust Network
-----BEGIN CERTIFICATE-----
MIIGLDCCBZWgAwIBAgIQbk/6s8XmacTRZ8mSq+hYxDANBgkqhkiG9w0BAQUFADCB
wTELMAkGA1UEBhMCVVMxFzAVBgNVBAoTDlZlcmlTaWduLCBJbmMuMTwwOgYDVQQL
EzNDbGFzcyAzIFB1YmxpYyBQcmltYXJ5IENlcnRpZmljYXRpb24gQXV0aG9yaXR5
IC0gRzIxOjA4BgNVBAsTMShjKSAxOTk4IFZlcmlTaWduLCBJbmMuIC0gRm9yIGF1
dGhvcml6ZWQgdXNlIG9ubHkxHzAdBgNVBAsTFlZlcmlTaWduIFRydXN0IE5ldHdv
cmswHhcNMDkwMzI1MDAwMDAwWhcNMTkwMzI0MjM1OTU5WjCBtTELMAkGA1UEBhMC
VVMxFzAVBgNVBAoTDlZlcmlTaWduLCBJbmMuMR8wHQYDVQQLExZWZXJpU2lnbiBU
cnVzdCBOZXR3b3JrMTswOQYDVQQLEzJUZXJtcyBvZiB1c2UgYXQgaHR0cHM6Ly93
d3cudmVyaXNpZ24uY29tL3JwYSAoYykwOTEvMC0GA1UEAxMmVmVyaVNpZ24gQ2xh
c3MgMyBTZWN1cmUgU2VydmVyIENBIC0gRzIwggEiMA0GCSqGSIb3DQEBAQUAA4IB
DwAwggEKAoIBAQDUVo9XOzcopkBj0pXVBXTatRlqltZxVy/iwDSMoJWzjOE3JPMu
7UNFBY6J1/raSrX4Po1Ox/lJUEU3QJ90qqBRVWHxYISJpZ6AjS+wIapFgsTPtBR/
RxUgKIKwaBLArlwH1/ZZzMtiVlxNSf8miKtUUTovStoOmOKJcrn892g8xB85essX
gfMMrQ/cYWIbEAsEHikYcV5iy0PevjG6cQIZTiapUdqMZGkD3pz9ff17Ybz8hHyI
XLTDe+1fK0YS8f0AAZqLW+mjBS6PLlve8xt4+GaRCMBeztWwNsrUqHugffkwer/4
3RlRKyC6/qfPoU6wZ/WAqiuDLtKOVImOHikLAgMBAAGjggKpMIICpTA0BggrBgEF
BQcBAQQoMCYwJAYIKwYBBQUHMAGGGGh0dHA6Ly9vY3NwLnZlcmlzaWduLmNvbTAS
BgNVHRMBAf8ECDAGAQH/AgEAMHAGA1UdIARpMGcwZQYLYIZIAYb4RQEHFwMwVjAo
BggrBgEFBQcCARYcaHR0cHM6Ly93d3cudmVyaXNpZ24uY29tL2NwczAqBggrBgEF
BQcCAjAeGhxodHRwczovL3d3dy52ZXJpc2lnbi5jb20vcnBhMDQGA1UdHwQtMCsw
KaAnoCWGI2h0dHA6Ly9jcmwudmVyaXNpZ24uY29tL3BjYTMtZzIuY3JsMA4GA1Ud
DwEB/wQEAwIBBjBtBggrBgEFBQcBDARhMF+hXaBbMFkwVzBVFglpbWFnZS9naWYw
ITAfMAcGBSsOAwIaBBSP5dMahqyNjmvDz4Bq1EgYLHsZLjAlFiNodHRwOi8vbG9n
by52ZXJpc2lnbi5jb20vdnNsb2dvLmdpZjApBgNVHREEIjAgpB4wHDEaMBgGA1UE
AxMRQ2xhc3MzQ0EyMDQ4LTEtNTIwHQYDVR0OBBYEFKXvCxHOwEEDo0plkEiyHOBX
LX1HMIHnBgNVHSMEgd8wgdyhgcekgcQwgcExCzAJBgNVBAYTAlVTMRcwFQYDVQQK
Ew5WZXJpU2lnbiwgSW5jLjE8MDoGA1UECxMzQ2xhc3MgMyBQdWJsaWMgUHJpbWFy
eSBDZXJ0aWZpY2F0aW9uIEF1dGhvcml0eSAtIEcyMTowOAYDVQQLEzEoYykgMTk5
OCBWZXJpU2lnbiwgSW5jLiAtIEZvciBhdXRob3JpemVkIHVzZSBvbmx5MR8wHQYD
VQQLExZWZXJpU2lnbiBUcnVzdCBOZXR3b3JrghB92f4Hz6getxB5Z/uniTTGMA0G
CSqGSIb3DQEBBQUAA4GBAGN0Lz1Tqi+X7CYRZhr+8d5BJxnSf9jBHPniOFY6H5Cu
OcUgdav4bC1nHynCIdcUiGNLsJsnY5H48KMBJLb7j+M9AgtvVP7UzNvWhb98lR5e
YhHB2QmcQrmy1KotmDojYMyimvFu6M+O0Ro8XhnF15s1sAIjJOUFuNWI4+D6ufRf
-----END CERTIFICATE-----
---
Server certificate
subject=/C=US/ST=California/L=San Jose/O=PayPal, Inc./OU=Information Systems/CN=paypal.com
issuer=/C=US/O=VeriSign, Inc./OU=VeriSign Trust Network/OU=Terms of use at https://www.verisign.com/rpa (c)09/CN=VeriSign Class 3 Secure Server CA - G2
---
No client certificate CA names sent
---
SSL handshake has read 3039 bytes and written 401 bytes
---
New, TLSv1/SSLv3, Cipher is DES-CBC3-SHA
Server public key is 1024 bit
Secure Renegotiation IS NOT supported
Compression: NONE
Expansion: NONE
SSL-Session:
    Protocol  : TLSv1
    Cipher    : DES-CBC3-SHA
    Session-ID: A8AAA4F22E9A4B3F12F76303464643525178846D96CA0BC0B81F35368BF55B89
    Session-ID-ctx: 
    Master-Key: 9F767B91FC2450E291CBB21E3438CA9A73FE8D5B825AD98F821F5EB912C088DFB66FCBF2D53591E2D1ED77E9B6A22504
    Key-Arg   : None
    PSK identity: None
    PSK identity hint: None
    Start Time: 1295242116
    Timeout   : 300 (sec)
    Verify return code: 20 (unable to get local issuer certificate)
---
^C
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Once a certificate has been obtained.  There are two options for adding the certificate.  The first involves updating the truststore for the JRE.  If that is not possible or not desirable, then one can set the truststore to be used by the jvm, using any arbitrary truststore that contains the appropriate certificate.

Both options require importing a certificate.  The following would import a certificate called, AD.cert into the `/etc/rundeck/ssl/truststore`.

    keytool -import -alias CompanyAD -file AD.cert -keystore /etc/rundeck/ssl/truststore -storepass adminadmin 

To add the certificate to the JRE, locate the file $JAVA_HOME/lib/security/cacerts and run

    keytool -import -alias CompanyAD -file AD.cert -keystore $JAVA_HOME/lib/security/cacerts -storepass changeit 

To verify your CA has been added, run keytool list and look for CompanyAD in the output.

    keytool -list -keystore $JAVA_HOME/lib/security/cacerts -storepass changeit

Refer to: http://download.oracle.com/javase/1.5.0/docs/tooldocs/solaris/keytool.html for more information how how to import a certificate.

Finally, in your `ldap-activedirectory.conf` be sure to change the *providerUrl* to be `ldaps://ad-server`.  Including the port is optional as the default is 686.

#### Redundant Connection Options

*providerUrl* can take multiple, space delimited, urls.  For example: 

     providerUrl=ldaps://ad1 ldaps://ad2  

Use this to provide connection redundancy if a particular host is unavailable.

## Authorization

Two dimensions of information dictate authorization inside RunDeck:

* *group* memberships assigned to a *user* login.  
* access control policy that grants access to one or more *policy
  action*s to a *group*  or *user*.

The section on [managing logins](#managing-logins) discusses how to
assign group memberships to users.

The remainder of this section will describe how to use the access
control policy.

*Note from the project team*: The authorization layer is an early work
 in progress. Please share your ideas on the IRC or mailing list.

### Access control policy

Access to running or modifying Jobs is managed in an access control
policy defined using the aclpolicy YAML document. 
This file contains a number of policy elements that describe what user
group is allowed to perform which actions.

Please read over this document for information on how to define it, and how to
grant access for certain actions to certain resources:

*  [aclpolicy-v10(5)](aclpolicy-v10.html)

Policies can be organized into more than one file to help organize
access by group or pattern of use. The normal RunDeck install will
have generated a policy for the "admin" group. Not all users will need
to be given "admin" access level to control and modify all Jobs. More
typically, a group of users will be given access to just a subset of
Jobs.

File listing: admin.aclpolicy example

    description: Admin project level access control. Applies to resources within a specific project.
    context:
      project: '.*' # all projects
    for:
      resource:
        - equals:
            kind: job
          allow: [create] # allow create jobs
        - equals:
            kind: node
          allow: [read,create,update,refresh] # allow refresh node sources
        - equals:
            kind: event
          allow: [read,create] # allow read/create events
      adhoc:
        - allow: [run,kill] # allow running/killing adhoc jobs
      job: 
        - allow: [read,update,delete,run,kill] # allow read/write/delete/run/kill of all jobs
      node:
        - allow: [read,run] # allow read/run for all nodes
    by:
      group: admin
    
    ---
    
    description: Admin Application level access control, applies to creating/deleting projects, admin of user profiles, viewing projects and reading system information.
    context:
      application: 'rundeck'
    for:
      resource:
        - equals:
            kind: project
          allow: [create] # allow create of projects
        - equals:
            kind: system
          allow: [read] # allow read of system info
        - equals:
            kind: user
          allow: [admin] # allow modify user profiles
      project:
        - match:
            name: '.*'
          allow: [read,admin] # allow view/admin of all projects
    by:
      group: admin

The example policy document above demonstrates the access granted to
the users in group "admin".

Two separate policies define two levels of access control.  The first is the "project"
context, which allows access to actions on resources within a specific project.
The second is the "application" level context, which allows access to things 
like creating projects, access to projects, managing users, and access to system
information.

### Specific Resources and Resource Types

As described in the [aclpolicy-v10(5)](aclpolicy-v10.html) definition, access
is granted or denied to specific "resources". Resources can take two forms:

* A specific resource, with a type and properties
* Resource types, which applies to all resources of a specific type or "kind"

For example, you might want to restrict access to a job or jobs within a certain 
group. This corresponds to specific "job" resources with a "group" property
matching a certain pattern.

You might also want to restrict who can create *new* jobs. Since a new job does 
not exist yet, you cannot create a rule for this action to apply to an existing 
job.  Which means this corresponds to a generic resource with a "kind" called "job".

### Special API Token Authentication group

Clients of the [Web API](api/index.html) may use the [Token Authentication](api/index.html#token-authentication) method.  These clients are
placed in the special authorization group called `api_token_group`.

`api_token_group`
~   Special role given to all [API Token](api/index.html#token-authentication) authenticated access.

### RunDeck resource authorizations

RunDeck declares a number of actions that can be referenced inside the access control policy document.

The actions and resources are divided into project scope and application scope:

#### Application Scope Resources and Actions

You define application scope rules in the aclpolicy, by declaring this context:

    context:
      application: 'rundeck'

These are the Application scope actions that can be allowed or denied via the
aclpolicy:

* Creating Projects ('create' action on a resource type with kind 'project')
* Reading system information ('read' action on a resource type with kind 'project')
* Administering user profiles ('admin' action on a resource type of kind 'user')
* Reading specific projects ('read' action on a project with a specific name)
* Administering specific projects ('admin' action on a project with a specific name

The following table summarizes the generic and specific resources and the 
actions you can restrict in the application scope:

Type       Resource Kind     Properties   Actions  Description
------     --------------    -----------  -------- ------------
`resource` `project`         none         `create` Create a new project
`resource` `system`          none         `read`   Read system information
`resource` `user`            none         `admin`  Modify user profiles
----------------------------

Table: Application scope generic type actions

Type      Properties   Actions  Description
-----     -----------  -------- ------------
`project` "name"       `read`   View a project in the project list
`project` "name"       `admin`  Modify project configuration
----------------------------

Table: Application scope specific resource actions


#### Project Scope Resources and Actions

You define project scope rules in the aclpolicy by declaring this context:

    context:
      project: "(regex)"

The regex can match all projects using ".*", or you can simply put the project
name.

Note that for projects not matched by an aclpolicy, *no* actions will be granted
to users.

Also note that to hide projects completely from users, you would need to grant
or deny the "read" access to the project in the [Application Scope](#application-scope-resources-and-actions).

These are the Application scope actions that can be allowed or denied via the
aclpolicy:

* Create Jobs ('create' action on a resource type with kind 'job')
* Read Node data ('read' action on a resource type with kind 'node')
* Update/Refresh node data ('create','update','refresh' action on a resource type with kind 'node')
* Read history events ('read' action on a resource type with kind 'event')
* Create history events ('create' action on a resource type with kind 'event')
* Run adhoc jobs ('run' action on 'adhoc' resources)
* Kill adhoc jobs ('kill' action on 'adhoc' resources)
* Any Action on Jobs (actions on 'job' resources, see below)

The following table summarizes the generic and specific resources and the 
actions you can restrict in the project scope:

Type       Resource Kind     Actions   Description
------     --------------    --------  ------------
`resource` `job`             `create`  Create a new Job
"          `node`            `read`    Read node information
"          "                 `create`  Create new node entries
"          "                 `update`  Modify node entries
"          "                 `refresh` Refresh node entry from a URL
"          `event`           `read`    Read history event information
"          "                 `create`  Create arbitrary history event entries
----------------------------

Table: Project scope generic type actions

Type      Properties                         Actions  Description
-----     -----------                        -------- ------------
`adhoc`                                      `run`    Run and adhoc execution
"                                            `kill`   Kill an adhoc execution
`job`     "name","group"                     `read`   View a Job
"                                            `update` Modify a job
"                                            `delete` Delete a job
"                                            `run`    Run a job
"                                            `kill`   Kill a running job
"                                            `create` Create the matching job
`node`    "rundeck_server", "nodename", ...  `read`   View the node in the UI
"                                            `run`    Run jobs/adhoc on the node
----------------------------

Table: Project scope specific resource actions

Recall that defining rules for a resource type is done in this way:

    for:
      resource:
        - equals:
            kind: 'project'
          allow: [create]

Whereas defining rules for specific resources of a certain type is done in this
way:

    for:
      job:
        - equals:
            name: bob
          allow: [run]

Note, for `node` resources, the properties available are all the attributes that are defined on the node, so you can apply authorizations based on tag, osName, hostname, etc. The special `rundeck_server` property will be set to "true" for the RunDeck server node only, and "false" for all other nodes.

#### Access control policy actions example

Below is an example policy document demonstrating policy actions
to create limited access for a group of users.
Users in the group "restart_user", are allowed to run three jobs in the "adm"
group, Restart, stop and start. By allowing `run` but not `read`,
the "stop" and "start" jobs will not be visible.

File listing: restart_user.aclpolicy example

    description: Limited user access for adm restart action
    context:
      project: '.*'
    for:
      job:
        - equals:
            group: 'adm'
            name: 'Restart'
          allow: [run,read]
        - equals:
            group: 'adm'
            name: 'stop'
          allow: [run]
        - equals:
            group: 'adm'
            name: 'start'
          allow: [run]
    by:
      group: [restart_user]

### Troubleshooting access control policy

After defining an aclpolicy file to grant access to a particular group
of users, you may find them getting "unauthorized" messages or
complaints that certain actions are not possible. 

To diagnose this, begin by checking two bits:

1. The user's group membership. This can be done by going to the
   user's profile page in RunDeck. That page will list the groups the
   user is a member. 
2. Read the messages inside the `rundeck.audit.log` log file. The
   authorization facility generates fairly low level messages describing
   how the policy is matched to the user context.

For each entry in the audit log, you'll see all decisions leading up to either a
AUTHORIZED or a REJECTED message.  It's not uncommon to see REJECTED
messages followed by AUTHORIZED.  The important thing is to look at
the last decision made.

### Authorization caveats

* aclpolicy changes do not require a restart.

## Configuring Rundeck for SSL

This document describes how to configure Rundeck for SSL/HTTPS
support, and assumes you are using the rundeck-launcher standalone
launcher.

(1) Before beginning, do a first-run of the launcher, as it will create
the base directory for Rundeck and generate configuration files.

        cd $RDECK_BASE;  java -jar rundeck-launcher-1.1.0.jar

This will start the server and generate necessary config files.  Press
control-c to shut down the server.

(2)  Using the [keytool] command, generate a keystore for use as the
server cert and client truststore. Specify passwords for key and keystore:

        keytool -keystore etc/keystore -alias rundeck -genkey -keyalg RSA -keypass admin -storepass admin
    
Be sure to specify the correct hostname of the server as the response
to the question "What is your first and last name?".  Answer "yes" to
the final question.

You can pass all the answers to the tool on the command-line by using
a HERE document.

Replace the first line "Venkman.local" with the hostname for your
server, and use any other organizational values you like:
    
        keytool -keystore etc/keystore -alias rundeck -genkey -keyalg RSA -keypass adminadmin -storepass adminadmin  <<!
        Venkman.local
        devops
        My org
        my city
        my state
        US
        yes
        !

[keytool]: http://linux.die.net/man/1/keytool-java-1.6.0-openjdk

(3) CLI tools that communicate to the Rundeck server need to trust the
SSL certificate provided by the server.  They are preconfigured to
look for a truststore at the location:
`$RDECK_BASE/etc/truststore`. Copy the keystore as the truststore for
CLI tools: 

        cp etc/keystore etc/truststore

(4) Modify the ssl.properties file to specify the location of the
keystore and the appropriate passwords:

        vi server/config/ssl.properties

An example ssl.properties file (from the RPM package).

        keystore=/etc/rundeck/ssl/keystore
        keystore.password=adminadmin
        key.password=adminadmin
        truststore=/etc/rundeck/ssl/truststore
        truststore.password=adminadmin
        
(5) Configure client properties.  Modify the file
`$RDECK_BASE/etc/framework.properties` and change these properties: 

    * `framework.server.url`
    * `framework.rundeck.url`
    * `framework.server.port` 
    
Set them to the appropriate https protocol, and change the port to
4443, or to the value of your `-Dserver.https.port` runtime
configuration property.

(6) Launch the rundeck launcher and tell it where to read the ssl.properties

        java -Drundeck.ssl.config=$RDECK_BASE/server/config/ssl.properties -jar rundeck-launcher-1.1.0.jar
    
You can change port by adding `-Dserver.https.port`:
    
        java -Drundeck.ssl.config=$RDECK_BASE/server/config/ssl.properties -Dserver.https.port=1234 rundeck-launcher-1.1.0.jar
    
If successful, you will see a line indicating the SSl connector has started:

    Started SslSocketConnector@0.0.0.0:4443

### Securing passwords

Passwords do not have to be stored in the ssl.config.  If they are not
set, then the server will prompt on the console for a user to enter
the passwords.

If you want the server to start without prompting then you need to set
the passwords in the config file.  

The passwords stored in ssl.properties can be obfuscated so they are
not in plaintext:

Run the jetty "Password" utility:

    $ java -cp server/lib/jetty-6.1.21.jar:server/lib/jetty-util-6.1.21.jar org.mortbay.jetty.security.Password <password>
    
This will produce two lines, one starting with "OBF:"

Use the entire OBF: output as the password in the ssl.properties file, eg:

    key.password=OBF:1lk2j1lkj321lj13lj
    

### Troubleshooting keystore

Some common error messages and causes:


java.io.IOException: Keystore was tampered with, or password was incorrect

:    A password specified in the file was incorrect.

2010-12-02 10:07:29.958::WARN:  failed SslSocketConnector@0.0.0.0:4443: java.io.FileNotFoundException: /Users/greg/rundeck/etc/keystore (No such file or directory)

:    The keystore/truststore file specified in ssl.properties doesn't exist


### Optional PEM export

You can export the PEM formatted server certificate for use by HTTPS
clients (web browsers or e.g. curl).


Export pem cacert for use by e.g. curl: 

    keytool -export -keystore etc/keystore -rfc -alias rundeck > rundeck.server.pem

## Customizing RunDeck GUI

You can modify some display features of the RunDeck GUI by setting
these properties in the [rundeck-config.properties](#rundeck-config.properties) file:

-------------------------------------------------------------------------------
**Property**              **Description**                      **Example**
----------------------    ----------------------------------   ----------------
`rundeck.gui.title`       Title shown in app header            Test App

`rundeck.gui.logo`        Logo icon path relative to           test-logo.png
                          webapps/rundeck/images dir           

`rundeck.gui.logo-width`  Icon width for proper display (32px  32px
                          is best)                             

`rundeck.gui.logo-height` Icon height for proper display (32px 32px
                          is best)                             

`rundeck.gui.titleLink`   URL for the link used by the app     http://rundeck.org
                          header icon.                         

`rundeck.gui.helpLink`    URL for the "help" link in the app   http://rundeck.org/
                          header.                              docs

`rundeck.gui.realJobTree` Displaying a real tree in the Jobs   false
                          overview instead of collapsing            
                          empty groups. **Default: true**           
-------------------------------------------------------------------------------


## Customizing configuration for scale

Note: This section is early in its development.

### File descriptors

The RunDeck server process opens a number of files during normal operation. These
include system and java libraries, logs, and sockets. 
Your system restricts the number of open file handles per process
but these limitations can be adjusted.

If your installation attempts to exceed the limit, you will see an error
like the one shown below in your [service.log](#logs) file.

    Too many open files 


_On Linux nodes_

List the current limit with the [ulimit](http://ss64.com/bash/ulimit.html) command:

    ulimit -n

If the limit is low (eg ``1024``) it should be raised.

You can get the current number of open file descriptors used by the 
RunDeck server process with [lsof](http://linux.die.net/man/8/lsof):

    losf -p <rundeck pid> | wc -l

Increase the limit for a wide margin. 
Edit [/etc/security/limits.conf](http://ss64.com/bash/limits.conf.html) file
to raise the hard and soft limits. Here they are raised to ``65535`` for 
the "rundeck" system account:

    rundeck hard nofile 65535
    rundeck soft nofile 65535


The system file descriptor limit is set in /proc/sys/fs/file-max. 
The following command will increase the limit to 65535:

    echo 65535 > /proc/sys/fs/file-max

In a new shell, run the ulimit command to set the new level:

    ulimit -n 65535

The ulimit setting can be set in the [rundeckd](#rundeckd) 
startup script, or [profile](#profile).

Restart RunDeck.

### Java heap size

The ``rundeckd`` startup script sets initial and maximum heap sizes
for the server process. For many installations it will be sufficient.

If the Rundeck JVM runs out of memory, the following error occurs:

    Exception in thread "main" java.lang.OutOfMemoryError: Java heap space

Heap size is governed by the following startup parameters: 
``-Xms<initial heap size>`` and ``-Xmx<maximum heap size>``


You can increase these by updating the RunDeck [profile](#profile). 
To see the current values, grep the ``profile`` for 
the Xmx and Xms patterns:

* Launcher installs:

        egrep '(Xmx|Xms)' $RDECK_BASE/etc/profile
   
* RPM installs:

        egrep '(Xmx|Xms)' /etc/rundeck/profile
   
The default settings initialized by the installer 
sets these to 1024 megabytes maximum
and 256 megabytes initial:

    export RDECK_JVM="$RDECK_JVM -Xmx1024m -Xms256m"

_Sizing advice_

Several factors drive memory usage in RunDeck:

* User sessions
* Concurrent threads
* Concurrent jobs
* Number of managed nodes

For example, if your installation has dozens of active users 
that manage a large environment (1000+ nodes), and has
sufficient system memory, the following sizings might be more suitable:

    export RDECK_JVM="$RDECK_JVM -Xmx4096m -Xms1024m -XX:MaxPermSize=256m"

### Quartz job threadCount

The maximum number of threads used by RunDeck for concurrent jobs 
is set in the ``quartz.properties`` file. By default, this is set to ``10``.

* RPM install: ``/var/lib/rundeck/server/exp/webapp/WEB-INF/classes/quartz.properties``
* Launcher install: ``$RDECK_BASE/server/exp/webapp/WEB-INF/classes/quartz.properties``

To change the maximum threadCount modify this file and alter the line:

    org.quartz.threadPool.threadCount = 20

Set the threadCount value to the max number of threads you want to run concurrently.

Please refer to the Quartz site for detailed information:
[Configuration - Thread Pool](http://www.quartz-scheduler.org/docs/1.x/configuration/ConfigThreadPool.html)

### JMX instrumentation

You may wish to monitor the internal operation of your RunDeck server via JMX.

JMX provides introspection on the JVM, the application server, 
and classes all through a consistent interface. 
These various components are exposed to the management console 
via JMX managed beans — MBeans for short.

_Note_: For more background information on JMX, see 
"[Java theory and practice: Instrumenting applications with JMX.](http://www.ibm.com/developerworks/library/j-jtp09196/)". 

Enable local JMX monitoring by adding the ``com.sun.management.jmxremote``
flag to the startup parameters in the [profile](#profile).

    export RDECK_JVM="$RDECK_JVM -Dcom.sun.management.jmxremote"

You use a JMX client to monitor JMX agents. 
This can be a desktop GUI like JConsole run locally.

    jconsole <rundeck pid>
    
For instructions on remote JMX monitoring for Grails, Spring and log4j see:
[Grails in the enterprise](http://www.ibm.com/developerworks/java/library/j-grails12168/index.html).