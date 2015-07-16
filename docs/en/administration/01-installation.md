% Installation

### System Architecture

Rundeck is a server application you host on a system you designate 
a central administrative control point. Internally, Rundeck stores job
definitions and execution history in a relational database. Output
from command and job executions is saved on disk but can be forwarded
to remote stores like S3 or Logstash. 

Rundeck distributed command execution is performed using a pluggable
node execution layer that defaults to SSH but plugins allow you
to use other means like MCollective, Salt, WinRM, or your custom method. 
Rundeck server configuration includes settings to define the outbound
user allowed by the remote hosts. Remote machines
are not required to make connections back to the server.

![Rundeck architecture](../figures/fig0001.png)

The Rundeck application itself is a Java-based webapp. The application provides both
graphical interface and network interfaces used by the Rundeck shell
tools. 

Access to the Rundeck application requires a login and
password. The default Rundeck installation uses a flat file user
directory containing a set of default logins. Logins are defined in
terms of a username and password as well as one or more user
groups. An alternative configuration to the flat file user directory,
is LDAP (e.g., ActiveDirectory) but Rundeck authentication and authorization
is customizable via [JAAS](http://en.wikipedia.org/wiki/Java_Authentication_and_Authorization_Service).
Users must also be authorized to perform actions like define a job
or execute one. This is controlled by an access control facility that reads
policy files defined by the Rundeck administrator. Privilege is
granted if a user's group membership meets the requirements of the policy.

Two installation methods are supported:

* System package: RPM and Debian packaging is intended for managed installation and provides
  robust tools that integrate with your environment, man pages, shell
  tool set in your path, init.d startup and shutdown.
  
* Launcher: The launcher is intended for quick setup, to get you
  running right away.  Perfect for bootstrapping a project or trying
  a new feature.  

Rundeck can also install as a WAR file into an external container like Tomcat.


Assuming the system requirements are met, Rundeck can be installed
either from source, system package or via the launcher.

### System Requirements

The following operating systems are known to support Rundeck:

* Linux: Most recent distributions are likely to work 
* Windows: XP, Server and above
* Mac OS X 10.4 or later

Root (or Administrator on Windows) is not required or recommended. We
recommend using a dedicated user account such as "rundeck".

If there is need for root access, please set up the Rundeck user
to have access via [sudo].

[sudo]: http://en.wikipedia.org/wiki/Sudo

#### Java

Rundeck is a Java-Servlet based server and therefore requires the Java
runtime.

The install process requires that the latest version of Java 1.7
be installed. Both the [Open JDK](http://openjdk.java.net/) and [Sun/Oracle](http://java.com/) JVMs can be used.
You must have the JAVA_HOME environment variable defined
in your environment before running the launcher.  The RPM will use the java found on your path. See [Setting JAVA_HOME](startup-and-shutdown.html#setting-java_home) if you want to run a different version of java.

Verify your Java version to check it meets the requirement:

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ {.bash}
$ java -version
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
java version "1.7.0_55"
OpenJDK Runtime Environment (rhel-2.4.7.1.el6_5-x86_64 u55-b13)
OpenJDK 64-Bit Server VM (build 24.51-b03, mixed mode)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 

#### Network access

When the server starts, it binds to several TCP ports by default:

*  4440 (http) 
*  4443 (https)

To check if the ports are free on a Unix host, run:

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ {.bash}
$ netstat -an | egrep '4440|4443' 
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

If the ports are in use on the server, you will see output similar to below:

    tcp46      0      0  *.4440                 *.*                    LISTEN

The installation procedures describe how to choose different ports, if
there is a conflict.
    
In addition, TCP port 22 (by default) needs to be open on the clients for SSH.
    
Clients should be set up to allow the Rundeck server user to connect to
the clients using SSH via public-key authentication. It should not
prompt for a password. See
[Configure remote machine for SSH](../plugins-user-guide/ssh-plugins.html#configuring-remote-machine-for-ssh)
in the Administration chapter.

There are various ways for installing SSH on Windows; we recommend
[Cygwin].

[Cygwin]: http://www.cygwin.com/
    
### Installing from Source

Checkout the sources from [GitHub](https://github.com/rundeck/rundeck)

You can build either the launcher jar (self-running archive), or a RPM.

    ./gradlew build

Creates artifacts:

* `rundeckapp/target/rundeck-X.Y.war`
* `rundeck-launcher/launcher/build/libs/rundeck-launcher-X.Y.jar`

Build the RPM:

    make rpm

To build clean:

    make clean

Documentation can be built using: `make clean docs`.  Documentation build requires [pandoc](http://johnmacfarlane.net/pandoc/).  The RPM build depends on the the 
documentation as well.

### Installing with RPM

Note: The latest install documentation is available at <http://rundeck.org/downloads.html>.

Note, the java JDK must be installed. Install any JDK that is 1.7+.

If you want to install Rundeck on Linux via a binary installer, you
can generally do so through the RPM tool that comes with your distribution. 

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ {.bash}
# rpm -i rundeck-2.x.x.noarch.rpm rundeck-config-2.x.x.noarch.rpm
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 

To install it using yum, first install the yum repo package and then
run yum install:

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ {.bash}
# rpm -Uvh http://repo.rundeck.org/latest.rpm
# yum install rundeck
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 

### Installing with Launcher

Use the launcher as an alternative to a system package:

1. Download the launcher jar file.
1. Define RDECK_BASE environment variable to the location of the install

    ~~~~~~~ {.bash}
    export RDECK_BASE=$HOME/rundeck; # or where you like it
    ~~~~~~~

1. Create the directory for the installation.

    ~~~~~~~ {.bash}
    mkdir -p $RDECK_BASE 
    ~~~~~~~

1. Copy the launcher jar to the installation directory.

    ~~~~~~~ {.bash}
    cp rundeck-launcher-2.0.0.jar $RDECK_BASE
    ~~~~~~~

1. Change directory and run the jar.

    ~~~~~~~ {.bash}
    cd $RDECK_BASE    
    java -XX:MaxPermSize=256m -Xmx1024m -jar rundeck-launcher-2.0.0.jar
    ~~~~~~~

1. Wait for the Started message.

    ~~~~~~~
    2010-11-19 13:35:51.127::INFO:  Started SocketConnector@0.0.0.0:4440
    ~~~~~~~

1. Update your shell environment 

    ~~~~~~~ {.bash}
    PATH=$PATH:$RDECK_BASE/tools/bin
    MANPATH=$MANPATH:$RDECK_BASE/docs/man
    ~~~~~~~


If you get an error message that resembles the one below, you probably
are using an unsupported Java version.

    Exception in thread "main" java.lang.UnsupportedClassVersionError: Bad version number in .class file

See the [startup and shutdown](startup-and-shutdown.html) section for
instructions on using the ``rundeckd`` shell tool to manage the 
rundeck launcher process.

#### Launcher Options

The launcher jar can take a number of options to specify how the server should start. If you execute with a "-h" you will see the usage information:

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ {.bash}
java -XX:MaxPermSize=256m -Xmx1024m -jar rundeck-launcher-2.1.0.jar -h
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 

    usage: java [JAVA_OPTIONS] -jar rundeck-launcher.jar  [-c PATH] [-d]
           [--installonly] [-s PATH] [-b PATH] [-p PATH] [-h] [-x PATH]
           [--skipinstall] [--serverdir PATH] [--datadir PATH]

    Run the rundeck server, installing the necessary components if they do not
    exist.
        --skipinstall         Skip the extraction of the utilities from the
                              launcher.
        --installonly         Perform installation only and do not start the
                              server.
     -b,--basedir <PATH>      The basedir
     -c,--configdir <PATH>    The location of the configuration.
     -d                       Show debug information
     -h,--help                Display this message.
     -p,--projectdir <PATH>   The location of Rundeck's project data.
     -s,--sbindir <PATH>      The install directory for the tools used by
                              administrators.
     -x,--bindir <PATH>       The install directory for the tools used by
                              users.
    
These options can be used to customize the directories used by the launcher. 
By default all the directories are organized by convention within the current
working directory where the launcher jar is located.

#### System Properties

You can also customize the launcher behavior by using some java system properties.

Specify these properties using the normal `-Dproperty=value` commandline options
to the `java` command:

* `server.http.port` The HTTP port to use for the server, default "4440"
* `server.https.port` The HTTPS port to use or the server, default "4443"
* `server.http.host` Address/hostname to listen on, default is all addresses "0.0.0.0"
* `server.hostname` Hostname to use for the server, default is the system hostname
* `server.web.context` Web context path to use, such as "/rundeck". Default is "/".
* `rdeck.base` Rundeck Basedir to use, default is the directory containing the launcher jar
* `server.datastore.path` Path to server datastore dir
* `default.user.name`  Username for default user account to create
* `default.user.password` Password for default user account to create
* `rundeck.jaaslogin` "true/false" - if true, enable JAAS login. If false, use the realm.properties file for login information.
* `loginmodule.name` Custom JAAS loginmodule name to use
* `loginmodule.conf.name` Name of a custom JAAS config file, located in the server's config dir.
* `rundeck.config.name` Name of a custom rundeck config file, located in the server's config dir.
* `rundeck.ssl.config` Path to the SSL config properties file to enable SSL. If not set, SSL is not enabled.
* `rundeck.jetty.connector.forwarded` true/false. Set to true to enable support for "X-forwarded-\*" headers which may be sent by a front-end proxy to the rundeck server. See [Using an SSL Terminated Proxy](configuring-ssl.html#using-an-ssl-terminated-proxy).
* `rundeck.jetty.connector.ssl.excludedProtocols` Comma-separated list of SSL protocols to disable. Default: 'SSLv3'. See [Disabling SSL Protocols](configuring-ssl.html#disabling-ssl-protocols).
* `rundeck.jetty.connector.ssl.includedProtocols` Comma-separated list of SSL protocols to include. Default is based on available protocols. See [Disabling SSL Protocols](configuring-ssl.html#disabling-ssl-protocols).
* `rundeck.jetty.connector.ssl.excludedCipherSuites` Comma-separated list of Cipher suites to disable. No default. See [Disabling SSL Protocols](configuring-ssl.html#disabling-ssl-protocols).
* `rundeck.jetty.connector.ssl.includedCipherSuites` Comma-separated list of Cipher suites to enable. Default is based on available cipher suites. See [Disabling SSL Protocols](configuring-ssl.html#disabling-ssl-protocols).

For more information about using SSL, see [Configuring Rundeck for SSL](configuring-ssl.html).

## First-Time Setup

### Logins 

Rundeck supports a number of user directory configurations. By
default, the installation uses a file based directory, but connectivity to
LDAP is also available. 
See [Administration - Authentication](../administration/authenticating-users.html).

The Rundeck installation process will have defined a set of temporary
logins useful during the getting started phase.

* `user`: Has access to run commands and jobs but unable to modify job
  definitions. Password: "user"
* `admin`: Belongs to the "admin" group and is automatically granted
  the "admin" and "user" role privileges. Password: "admin"
  
### Group membership

If you installed Rundeck using the RPM installation method, it will
have created a unix group called "rundeck".

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 
$ groups rundeck
rundeck : rundeck
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 

It also made several log files writable to members of the "rundeck" group.

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ {.bash}
$ ls -l /var/log/rundeck/command.log
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 
-rw-rw-r-- 1 rundeck rundeck 588 Dec  2 11:24 /var/log/rundeck/command.log
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 

If you want to use the Rundeck shell tools, be sure to add that group
to the necessary user accounts.

Rundeck shell tool users that do not belong to group, rundeck, will
get error messages like so:

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ {.bash}
$ rd-jobs
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 
log4j:ERROR setFile(null,true) call failed. java.io.FileNotFoundException: /var/log/rundeck/command.log (Permission denied)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 

Consult the [usermod] command to modify a user account.

[usermod]: http://linux.die.net/man/8/usermod

