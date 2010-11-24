% RUNDECK(1) RunDeck User Manuals | Version 1.0
% Alex Honor
% November 20, 2010

# Getting Started #

This chapter will be about getting started with RunDeck. We will begin
by explaining ... , then move on to ..., finally ... At the end of
this chapter you should understand what RunDeck is, how you should use
it and you should be all setup to do so.


## RunDeck Basics ##

Several fundamental concepts underly and drive the development of the
RunDeck system. If you are a new user, knowing about them will
help you use and integrate RunDeck into your environment.

### Command dispatching ###

RunDeck supports a notion called Command dispatching wherein a
user specifies dispatch critera along with an action (called a
command) and this specification is used to perform a distributed execution.

Internally, a mechanism called the command dispatcher does
a lookup to find node resources meeting specified filtering criteria and
performs the distributed command execution. The command executes in a data
context that contains information about the Node resource.

The command dispatcher supports two methods of command execution:

* Ad hoc commands: Execute any shell command or shell script across a
  set of hosts.  
* Jobs: Encapsulate commands as a named Job and tie them
  together into Job workflows.   

RunDeck provides both graphical and command line interfaces to
interact with the command dispatcher.

### Resource model ###

The command dispatcher works in conjunction with a resource model. A
resource model is a representation of hosts deployed in your
network. A _Node_ resource is either a physical or virtual instance
of an operating system that is network accessible.

Nodes have a number of basic properties but these properties can be
extended to include arbitrary named key value pairs.

You can configure RunDeck to retrieve and store resource model data
from any source, so long as it meets the RunDeck resource model
document requirement.

### Authorization ###

RunDeck uses an authorization model where users belong to groups and
those groups are associated with abitrarily defined roles.

Every action executed through the RunDeck command dispatcher must meet
the requirements of an access control policy definition. 

Since RunDeck respects the ACLs definition, you can use role-based
authorization to restrict some users to only a subset of actions. This
provides a Self-Service type interface, where some users can have
access to a limited set of actions to execute.

### Project ###

All RunDeck activities occur within the context of a single project.
Each project has its own resource model and Job store.

Multiple projects can be maintained on the same RunDeck server.
Projects are independent of one another, so you can use them to
organize unrelated systems within a single RunDeck
installation. This can be useful for managing different infrastructures.

## Installing RunDeck ##

Assuming the system requirements are met, RunDeck can be installed
either from source, system package or via the launcher.

### System Requirements ###

The following operating systems are known to support RunDeck:

* Linux: Most recent distributions are likely to work
* Windows: XP, Server and above
* Mac OS X 10.4 or later
* Solaris or OpenSolaris

Root (or Administrator on Windows) is not required or recommended. We
recommend using a dedicated user account such as "rundeck".

If there is need for root access, please set up the RunDeck user
to have access via sudo.

#### Java ####

RunDeck is a Java-Servlet based server and therefore requires the Java
runtime.

The install process requires that the latest version of Java 1.5 or 1.6
be installed. Both the [Open JDK](http://openjdk.java.net/) and [Sun/Oracle](http://java.com/) JVMs can be used.
You must have the JAVA_HOME environment variable defined
in your environment before running the install script. 

Verify your Java version to check it meets the requirement:

    $ java -version
    java version "1.6.0_22"
    Java(TM) SE Runtime Environment (build 1.6.0_22-b04-307-10M3261)
    Java HotSpot(TM) 64-Bit Server VM (build 17.1-b03-307, mixed mode)

#### Network access ####

Cients should be set up to allow the RunDeck server user to connect to
the clients using SSH via public-key authentication. It should not
prompt for a password. There are various ways of installing SSH on
Windows; we recommend [Cygwin](http://www.cygwin.com/).

TCP ports 8080 and 1055 need to be open on the server. In addition,
TCP port 22 needs to be open on the clients for SSH.

To check the ports are free on a Unix host run:

    netstat -an | egrep '8080|1055' 

If the ports are in use on the server, you will see output similar to below:

    tcp46      0      0  *.8080                 *.*                    LISTEN
    tcp46      0      0  *.1055                 *.*                    LISTEN

The installation procedures describe how to choose different ports, if
there is a conflict.
    
### Installing from Source ###

Checkout the sources from GitHub: https://github.com/dtolabs/rundeck

Run the build script:

    ./build.sh

Build clean

    ./build.sh -clean

### Installing on Linux ###

If you want to install RunDeck on Linux via a binary installer, you can generally do so through the basic package-management tool that comes with your distribution. If you’re on Fedora, you can use yum:

    $ yum install rundeck

### Installing on other platforms ###

Use the launcher as an alternative to a system package:

1. Download the launcher jar file.

1. Create a directory for the installation.

    ~~~~~~~
    mkdir $HOME/rundeck 
    ~~~~~~~

1. Copy the launcher jar to the installation directory.

    ~~~~~~~
    cp rundeck-launcher-1.0.0.jar $HOME/rundeck
    ~~~~~~~

1. Change directory and start the jar.

    ~~~~~~~
    cd $HOME/rundeck    
    /usr/bin/java -jar rundeck-launcher-1.0.0.jar
    ~~~~~~~

1. Wait for the Started message.

    ~~~~~~~
    2010-11-19 13:35:51.127::INFO:  Started SocketConnector@0.0.0.0:8080
    ~~~~~~~

## First-Time Setup ##

### Logins ###

RunDeck supports a number of user directory configurations. By
default, the installation uses a file based directory, but connectivity to
LDAP is also available.

The RunDeck installation process will have defined a set of initial
logins useful during the getting started phase.

* admin: Belongs to the "admin" group and is automatically granted
  the "admin" role privileges.
* deploy: Has access to run commands and jobs but unable to modify job
  definitions.
  
## Getting Help ##

RunDeck includes a set of Unix manual pages describing the shell
tools.

For Linux users read the introductory man page:

    $ man rundeck

For those who installed the launcher add the man pages to your MANPATH

    MANPATH=$MANPATH:$HOME/rundeck/man

  
## Summary ##

You should have a basic understanding of what RunDeck. You
should also now have a working version of RunDeck on your system
that’s set up with your personal identity. It’s now time to learn some
RunDeck basics.

  
