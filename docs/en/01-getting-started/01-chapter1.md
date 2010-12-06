% RUNDECK(1) RunDeck User Manuals | Version 1.0
% Alex Honor
% November 20, 2010

# Getting Started

This chapter helps new users getting started with RunDeck. We will begin
by explaining the basics, covering essential RunDeck concepts and
terminology and then move on to installation and finally, setup.
At the end of this chapter you should understand what RunDeck is, how
you should use it and you should be all setup to do so.

## RunDeck Basics

Several fundamental concepts underly and drive the development of the
RunDeck system. If you are a new user, knowing about them will
help you use and integrate RunDeck into your environment.

### Command dispatching

RunDeck supports a notion called Command dispatching wherein a
user specifies dispatch critera along with an action (called a
command) and this specification is used to perform a distributed execution.

The *command dispatcher* is an internal mechanism that looks up
node resources meeting specified filtering criteria and then
performs the distributed command execution. The command executes in a data
context that contains information about the Node resource. Besides
node filtering, dispatcher options include parameters to control
parallel execution, ordering and error handling.

The command dispatcher supports two methods of command execution:

* *Ad-hoc commands*: Execute any shell command or shell script across a
  set of hosts.  
* *Jobs*: Encapsulate commands as a named Job and tie them
  together into multi-step workflows.   

RunDeck provides both graphical and command line interfaces to
interact with the command dispatcher.

### Resource model

The command dispatcher works in conjunction with a resource model. A
*resource model* is a representation of hosts deployed in your
network. A _Node_  is a resource that is either a physical or virtual instance
of a network accessible host.

Nodes have a number of basic properties but these properties can be
extended to include arbitrary named key value pairs.

You can configure RunDeck to retrieve and store resource model data
from any source, so long as it meets the RunDeck resource model
document requirement. 

A *resource model provider* is an external service
accesible via the HTTP GET method that returns data conforming to the
RunDeck resources document format (resource-v10(5)). 


### Authorization

RunDeck enforces an *access control policy* that grants certain
privileges to groups of users.
Every action executed through the RunDeck command dispatcher must meet
the requirements of an access control policy definition. 

Since RunDeck respects the policy definition, you can use role-based
authorization to restrict some users to only a subset of actions. This
provides a self-service type interface, where some users can have
access to a limited set of actions to execute.

### Project

A *project* is a place to separate management activity.
All RunDeck activities occur within the context of a project.
Each project has its own resource model and Job store.

Multiple projects can be maintained on the same RunDeck server.
Projects are independent from one another, so you can use them to
organize unrelated systems within a single RunDeck
installation. This can be useful for managing different infrastructures.

## Installing RunDeck

Assuming the system requirements are met, RunDeck can be installed
either from source, system package or via the launcher.

### System Requirements

The following operating systems are known to support RunDeck:

* Linux: Most recent distributions are likely to work 
* Windows: XP, Server and above
* Mac OS X 10.4 or later

Root (or Administrator on Windows) is not required or recommended. We
recommend using a dedicated user account such as "rundeck".

If there is need for root access, please set up the RunDeck user
to have access via [sudo].

[sudo]: http://en.wikipedia.org/wiki/Sudo

#### Java

RunDeck is a Java-Servlet based server and therefore requires the Java
runtime.

The install process requires that the latest version of Java 1.6
be installed. Both the [Open JDK](http://openjdk.java.net/) and [Sun/Oracle](http://java.com/) JVMs can be used.
You must have the JAVA_HOME environment variable defined
in your environment before running the installation. 

Verify your Java version to check it meets the requirement:

    $ java -version
    java version "1.6.0_22"
    Java(TM) SE Runtime Environment (build 1.6.0_22-b04-307-10M3261)
    Java HotSpot(TM) 64-Bit Server VM (build 17.1-b03-307, mixed mode)

#### Network access

When the server starts, it binds to several TCP ports:

*  4440 (http) 
*  4443 (https)
*  4435 (log4j)

To check the ports are free on a Unix host run:

    netstat -an | egrep '4440|4435' 

If the ports are in use on the server, you will see output similar to below:

    tcp46      0      0  *.4440                 *.*                    LISTEN
    tcp46      0      0  *.4435                 *.*                    LISTEN

The installation procedures describe how to choose different ports, if
there is a conflict.
    
In addition, TCP port 22 needs to be open on the clients for SSH.
    
Cients should be set up to allow the RunDeck server user to connect to
the clients using SSH via public-key authentication. It should not
prompt for a password. See
[Configure remote machine for SSH](#configuring-remote-machine-for-ssh)
in the Administration chapter.

There are various ways for installing SSH on Windows; we recommend
[Cygwin].

[Cygwin]: http://www.cygwin.com/
    
### Installing from Source

Checkout the sources from [GitHub](https://github.com/dtolabs/rundeck)

Run the build script:

    ./build.sh

Build clean

    ./build.sh -clean

The build will generate a launcher jar. On Linux build servers, an RPM
will also be generated.

### Installing with RPM

If you want to install RunDeck on Linux via a binary installer, you
can generally do so through the RPM tool that comes with your distribution. 

    $ rpm -i rundeck-1.0.0.noarch.rpm

To install it using yum:
    
    $ yum install rundeck

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
    cp rundeck-launcher-1.0.0.jar $RDECK_BASE
    ~~~~~~~

1. Change directory and run the jar.

    ~~~~~~~
    cd $RDECK_BASE    
    java -jar rundeck-launcher-1.0.0.jar
    ~~~~~~~

1. Wait for the Started message.

    ~~~~~~~
    2010-11-19 13:35:51.127::INFO:  Started SocketConnector@0.0.0.0:4440
    ~~~~~~~

1. Update your shell environment 

    ~~~~~~~
    PATH=$PATH:$RDECK_BASE/tools/bin
    MANPATH=$MANPATH:$RDECK_BASE/man
    ~~~~~~~


If you get an error message that resembles the one below, you probably
are using an unupported Java version.

    Exception in thread "main" java.lang.UnsupportedClassVersionError: Bad version number in .class file

## First-Time Setup

### Logins 

RunDeck supports a number of user directory configurations. By
default, the installation uses a file based directory, but connectivity to
LDAP is also available. See [Managing logins](#managing-logins) in the
Administration chapter.

The RunDeck installation process will have defined a set of temporary
logins useful during the getting started phase.

* `user`: Has access to run commands and jobs but unable to modify job
  definitions. Password: "user"
* `admin`: Belongs to the "admin" group and is automatically granted
  the "admin" and "user" role privileges. Password: "admin"
  
### Group membership

If you installed RunDeck using the RPM installation method, it will
have created a unix group called "rundeck".

    $ groups rundeck
    rundeck : rundeck

It also made several log files writable to members of the "rundeck" group.

    $ ls -l /var/log/rundeck/command.log
    -rw-rw-r-- 1 rundeck rundeck 588 Dec  2 11:24 /var/log/rundeck/command.log

If you want to use the RunDeck shell tools, be sure to add that group
to the necessary user accounts.

RunDeck shell tool users that do not belong to group, rundeck, will
get error messages like so:

    $ rd-jobs
    log4j:ERROR setFile(null,true) call failed. java.io.FileNotFoundException: /var/log/rundeck/command.log (Permission denied)

Consult the [usermod] command to modify a user account.

[usermod]: http://linux.die.net/man/8/usermod

## Summary 

You should now have a basic understanding of RunDeck. You
should also have a working version of RunDeck on your system
and login access. It is now time to learn some RunDeck basics.

  
