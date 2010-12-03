% RUNDECK(1) RunDeck User Manuals | Version 1.0
% Alex Honor
% November 20, 2010

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
    
### Launcher

The Launcher installation generates the script into the RDECK_BASE directory.
You may choose to incorporate it into your server's boot process.

The script is located here: `$RDECK_BASE/server/sbin/rundeckd`.

*Startup*

    $RDECK_BASE/server/sbin/rundeckd start

*Shutdown*

    $RDECK_BASE/server/sbin/rundeckd stop

## Configuration

### Directory layout

Configuration file layout differs between the RPM and Launcher
installation methods.

#### RPM layout
    /etc/rundeck
    |-- client
    |   |-- admin.aclpolicy
    |   |-- framework.properties
    |   |-- log4j.properties
    |   |-- profile
    |   `-- project.properties
    `-- server
        |-- jaas-loginmodule.conf
        |-- log4j.properties
        |-- realm.properties
        `-- rundeck-config.properties

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

`admin.aclpolicy`
~   Access control policy for "admin" group (man aclpolicy(5))
`framework.properties`
~   Configuration used by shell tools
`log4j.properties`
~   Logging configuration file
`profile`
~   Shell environment variables
`project.properties`
~   Project creation bootstrap config
`jaas-loginmodule.conf`
~   User realm configuration
`realm.properties`
~   User realm logins
`rundeck-config.properties`
~   The RunDeck webapp configuration

## Logs

Dependeing on the installer used, the log files will be under a base
directory:

*   RPM: `/var/log/rundeck`
    - `rundeck.audit.log`: Authorization messages
    - `rundeck.log`: Application messages
    - `service.log`: Standard input and output
*   Launcher: `$RDECK_BASE/var/logs`
    -   `service.log`: All output is logged here


## Backup and recovery

RunDeck backup should only be done with the server down. 

(1) Stop the server

(2) Copy the data files:

* RPM install: `/var/lib/rundeck/data`
* Launcher install: `$RDECK_BASE/server/data`

(3) Export the jobs

    rd-jobs -f jobs.xml
    
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
* SSH should not prompt for a password. There are many resources
available on how to configure ssh to use public key authentication
instead of passwords such as:
[Password-less logins with OpenSSH](http://www.debian-administration.org/articles/152)
or
[How-To: Password-less SSH](http://www.cs.wustl.edu/~mdeters/how-to/ssh/)

### SSH key generation

* By default, the RunDeck framework is configured to use DSA _not_ RSA
  type keys (however, it can be configured to use RSA, if required).
  
Here's an example of SSH DSA key generation on a Linux system:

    $ ssh-keygen -t dsa
    Generating public/private dsa key pair.
    Enter file in which to save the key (/home/demo/.ssh/id_dsa): 
    Enter passphrase (empty for no passphrase): 
    Enter same passphrase again: 
    Your identification has been saved in /home/demo/.ssh/id_dsa.
    Your public key has been saved in /home/demo/.ssh/id_dsa.pub.
    The key fingerprint is:
    37:d6:3a:b6:17:db:e2:2f:84:ca:b2:ed:7a:43:0d:26 


### Configuring remote machine for SSH 

To be able to directly ssh to remote machines, the SSH public key of
the client should be shared to the remote machine.
  
Follow the steps given below to enable ssh to remote machines.

The ssh public key should be copied to the `authorized_keys` file of
the remote machine. The public key will be available in
`~/etc/id_dsa.pub` file.
  
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
Cygwin on Windows as it includes SSH and a number of
Unix-like tools that are useful when you work in a command line
environment.

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
WARNING: UNTESTED & INCOMPLETE

(1)  Setup the LDAP login module configuration file

Depending on the installer create the `ldap-loginModule.conf` file 

* RPM install: /etc/runduck/server/config
* Launcher install: $RDECK_BASE/server/config

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
ldaploginmodule {
    org.mortbay.jetty.plus.jaas.spi.LdapLoginModule required
    debug="true"
    contextFactory="com.sun.jndi.ldap.LdapCtxFactory"
    hostname="localhost"
    port="3890"
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
    roleObjectClass="group";
    };
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

(2) Update profile
Update the `CONFIG_PROPS` in the RunDeck server's
`$RDECK_BASE/etc/profile.orig` to include setting the
`java.security.auth.login.config` property:



~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
$ diff $RDECK_BASE/etc/profile $RDECK_BASE/etc/profile.orig
14c14
> CONFIG_PROPS="-Drundeck.config.location=/home/chuck/rundeck/rundeck-config.properties -Djava.security.auth.login.config=etc/ldap-loginModule.conf"
---
< CONFIG_PROPS="-Drundeck.config.location=/home/chuck/rundeck/rundeck-config.properties"
---
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

(Re)source your environment by logging out and logging in again or by
directly sourcing the `$RDECK_BASE/etc/profile` file:

    $ source $RDECK_BASE/etc/profile

RunDeck should now be configured for authentication and role
membership using the Active Directory server.

## Authorization

### Role mapping

### Acess control policy


## Configuring Rundeck for SSL

This document describes how to configure Rundeck for SSL/HTTPS
support, and assumes you are using the rundeck-launcher standalone
launcher.

(1) Before beginning, do a first-run of the launcher, as it will create
the base directory for Rundeck and generate configuration files.

        cd $RDECK_BASE;  java -jar rundeck-launcher-1.0.0.jar

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
    
        keytool -keystore etc/keystore -alias rundeck -genkey -keyalg RSA -keypass admin -storepass admin  <<!
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
    
(5) Configure client properties.  Modify the file
`$RDECK_BASE/etc/framework.properties` and change these properties: 

    * `framework.server.url`
    * `framework.rundeck.url`
    * `framework.server.port` 
    
Set them to the appropriate https protocol, and change the port to
4443, or to the value of your `-Dserver.https.port` runtime
configuration property.

(6) Launch the rundeck launcher and tell it where to read the ssl.properties

        java -Drundeck.ssl.config=$RDECK_BASE/server/config/ssl.properties -jar rundeck-launcher-1.0.0.jar
    
You can change port by adding `-Dserver.https.port`:
    
        java -Drundeck.ssl.config=$RDECK_BASE/server/config/ssl.properties -Dserver.https.port=1234 rundeck-launcher-1.0.0.jar
    
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
    

### Troubleshooting

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