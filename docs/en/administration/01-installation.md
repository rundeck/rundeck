% Installation

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

The install process requires that the latest version of Java 1.6
be installed. Both the [Open JDK](http://openjdk.java.net/) and [Sun/Oracle](http://java.com/) JVMs can be used.
You must have the JAVA_HOME environment variable defined
in your environment before running the launcher.  The RPM will use the java found on your path. See [Setting JAVA_HOME](startup-and-shutdown.html#setting-java_home) if you want to run a different version of java.

Verify your Java version to check it meets the requirement:

    $ java -version
    java version "1.6.0_22"
    Java(TM) SE Runtime Environment (build 1.6.0_22-b04-307-10M3261)
    Java HotSpot(TM) 64-Bit Server VM (build 17.1-b03-307, mixed mode)

#### Network access

When the server starts, it binds to several TCP ports by default:

*  4440 (http) 
*  4443 (https)

To check if the ports are free on a Unix host, run:

    netstat -an | egrep '4440|4443' 

If the ports are in use on the server, you will see output similar to below:

    tcp46      0      0  *.4440                 *.*                    LISTEN

The installation procedures describe how to choose different ports, if
there is a conflict.
    
In addition, TCP port 22 (by default) needs to be open on the clients for SSH.
    
Clients should be set up to allow the Rundeck server user to connect to
the clients using SSH via public-key authentication. It should not
prompt for a password. See
[Configure remote machine for SSH](ssh.html#configuring-remote-machine-for-ssh)
in the Administration chapter.

There are various ways for installing SSH on Windows; we recommend
[Cygwin].

[Cygwin]: http://www.cygwin.com/
    
### Installing from Source

Checkout the sources from [GitHub](https://github.com/dtolabs/rundeck)

You can build either the launcher jar (self-running archive), or a RPM.

    make

Creates the rundeck-launcher.jar

Build the RPM:

    make rpm

To build clean:

    make clean

Documentation can be built using: `make clean docs`.  Documentation build requires [pandoc](http://johnmacfarlane.net/pandoc/).  The RPM build depends on the the 
documentation as well.

### Installing with RPM

If you want to install Rundeck on Linux via a binary installer, you
can generally do so through the RPM tool that comes with your distribution. 

    # rpm -i rundeck-1.1.0.noarch.rpm

To install it using yum, first install the yum repo package and then
run yum install:

    # rpm -Uvh http://rundeck.org/latest.rpm
    # yum install rundeck

### Installing with Launcher

Use the launcher as an alternative to a system package:

1. Download the launcher jar file.
1. Define RDECK_BASE environment variable to the location of the install

    ~~~~~~~
    export RDECK_BASE=$HOME/rundeck; # or where you like it
    ~~~~~~~

1. Create the directory for the installation.

    ~~~~~~~
    mkdir -p $RDECK_BASE 
    ~~~~~~~

1. Copy the launcher jar to the installation directory.

    ~~~~~~~
    cp rundeck-launcher-1.1.0.jar $RDECK_BASE
    ~~~~~~~

1. Change directory and run the jar.

    ~~~~~~~
    cd $RDECK_BASE    
    java -jar rundeck-launcher-1.1.0.jar
    ~~~~~~~

1. Wait for the Started message.

    ~~~~~~~
    2010-11-19 13:35:51.127::INFO:  Started SocketConnector@0.0.0.0:4440
    ~~~~~~~

1. Update your shell environment 

    ~~~~~~~
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

    java -jar rundeck-launcher-1.3.0.jar -h

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

For more information about using SSL, see [Configuring Rundeck for SSL](ssl.html).
