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

The install process requires that the latest version of Java 8
be installed. Both the [Open JDK](http://openjdk.java.net/) and [Sun/Oracle](http://java.com/) JVMs can be used.
You must have the JAVA_HOME environment variable defined
in your environment before running the launcher.  The RPM will use the java found on your path. See [Setting JAVA_HOME](startup-and-shutdown.html#setting-java_home) if you want to run a different version of java.

Verify your Java version to check it meets the requirement:

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ {.bash}
$ java -version
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
java version "1.8.0_131"
Java(TM) SE Runtime Environment (build 1.8.0_131-b11)
Java HotSpot(TM) 64-Bit Server VM (build 25.131-b11, mixed mode)
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

Note, the Java 8 JDK must be installed.

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

Consult the [usermod] command to modify a user account.

[usermod]: http://linux.die.net/man/8/usermod

## Install on Windows

### Install the launcher as a service

To install the launcher on Windows as a service, follow these instructions: 

* Choose a root directory (e.g. `C:\rundeck`)
* Place rundeck-launcher-x.x.x.jar in that directory
* Define RDECK_BASE environment variable( e.g `set RDECK_BASE=C:\rundeck`) 
* cd to the RDECK_BASE and launch the installation of rundeck

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ {.bash}
    cd C:\rundeck
    java -jar rundeck-launcher-x.x.x.jar --installonly
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 

* This will create the usual structure under `%RDECK_BASE%`, but will just install it, not execute it.
* If `--installonly` doesn't create all folders, just run `java -jar rundeck-launcher-x.x.x.jar` and stop the instance after the startup process finished.
* Customize `%RDECK_BASE%\etc\profile.bat` according to your installation (for example `-Xmx4096m -Xms1024m`)
* Customize `%RDECK_BASE%\server\config\rundeck-config.properties` accordingly to the docs
* Create a bat file (e.g. `start_rundeck.bat`) and place it under `%RDECK_BASE%`

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ {.bash}
    set CURDIR=%~dp0
    call %CURDIR%etc\profile.bat
    java %RDECK_CLI_OPTS% %RDECK_SSL_OPTS% -jar rundeck-launcher-2.6.10.jar --skipinstall -d
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 
    
* fetch `nssm.exe` from [http://nssm.cc/](http://nssm.cc/)
* Place the executable under `%RDECK_BASE%` (you can place it elsewhere, but for the sake of the example let's use always the root dir)
* Open a prompt and issue these commands (Administrator mode required to install a service)

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ {.bash}
    cd C:\rundeck
    nssm.exe install RUNDECK
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 
    
* the GUI pops up, set "path" as `%RDECK_BASE%\start_rundeck.bat` , working dir as `%RDECK_BASE%` (optionally set "low" on the process tab, under priority, to avoid server cpu spike when starting rundeck)
* Go to the service management console (services.msc) and you'll find RUNDECK listed as a service. Starting it will start the rundeck process.



###IIS as a reverse proxy

This steps allow to "integrate" rundeck with IIS. This could be necessary for example for these user cases:

* to get rundeck through the port 80 in a subdirectory of IIS (e.g. `http://yoursite/rundeck`)
* when It is already set up a SSL certificate for a site and you want to reuse it without fiddling with the java certstore

If you are at least on 2008R2, you can install the [ARR extension](https://www.iis.net/downloads/microsoft/application-request-routing) to proxy requests to the Jetty instance.

To do so, we need first to modify the port rundeck is running and specify the "prefix" for all urls generated by rundeck. Also, to avoid that anyone bypass IIS, rundeck must listen only on 127.0.0.1. 

Modify the `%RDECK_BASE%\start_rundeck.bat` port (81) and host (127.0.0.1) as follows:


    set CURDIR=%~dp0
    call %CURDIR%etc\profile.bat
    java %RDECK_CLI_OPTS% %RDECK_SSL_OPTS%  "-Dserver.http.host=127.0.0.1" "-Dserver.http.port=81" -jar rundeck-launcher-x.x.x.jar --installonly


But you can also put options in `%RDECK_BASE%\etc\profile.bat` (in this case the "prefix" and the little bit that makes rundeck work behind a reverse proxy) as long as you define all of them.


    ...
    set RDECK_CLI_OPTS=-Xmx4096m -Xms1024m "-Drundeck.jetty.connector.forwarded=true" "-Dserver.web.context=/rundeck"
    ...


A tune is also needed in `%RDECK_BASE%\server\config\rundeck-config.properties`, as `grails.serverURL` is needed to be the absolute url:


    ...
    grails.serverURL=http://yoursite/rundeck
    ...


Now, in IIS, (at the site level) create an URL Rewrite rule as follows:

* add inbound rule
* name: `route_to_rdeck` (or whatever you prefer)
matches the pattern (using regular expression): `rundeck.*` (or a regex matching your "prefix")
* action type: `rewrite`
* rewrite url: `http://127.0.0.1:81/{R:0}`
* check `append query string`

Then, any request coming to http://yoursite/rundeck will be proxying to your rundeck instance running on port 81 on the same server, and you'll be sure that every request will come in only proxied by IIS, since rundeck is only listening to 127.0.0.1.

#### Troubleshooting

If the rewrite module does not work, go to the `Application Request Routing` settings on the IIS home, and enable the proxy. 

### MSSQL as a backend

Rundeck works with MSSQL too:

* Create a database on your backend, plus a user (and assign it the db_owner role)
* Download JDBC driver from Microsoft (For example for a Windows 2012 R2 host connecting to a MSSQL 2014 server, use `sqljdbc41.jar` (572KB, mod date 2015-11-18) )
* Place it under `%RDECK_BASE%\server\lib`
* in `%RDECK_BASE%\server\config\rundeck-config.properties`, set the following:

~~~~~~~ {.bash}
...
rundeck.projectsStorageType=db
dataSource.dbCreate = update
dataSource.driverClassName = com.microsoft.sqlserver.jdbc.SQLServerDriver
dataSource.url = jdbc:sqlserver://myserver;DatabaseName=RUNDECK
dataSource.username = myusername
dataSource.password = mypassword
...
~~~~~~~


### Updating rundeck using the launcher

When you need to update rundeck and you can not find the relevant section on the docs you are probably on a quite recent version. This worked successfully in all the steps from 2.6.5 to 2.6.10.

* Stop rundeck
* Remove `%RDECK_BASE%\server\exp` and `%RDECK_BASE%\server\lib`, making a backup of any file you modified under those directories (e.g. `%RDECK_BASE%\server\exp\webapp\WEB-INF\classes\log4j.properties`, `%RDECK_BASE%\server\exp\webapp\WEB-INF\web.xml`, etc)
* download the launcher
* open a prompt, optionally setting RDECK_BASE and launch --installonly

    java -jar rundeck-launcher-2.6.10.jar --installonly

* copy over your customizations
* don't forget, e.g., sqljdbc41.jar in `%RDECK_BASE%\server\lib`
* start rundeck

