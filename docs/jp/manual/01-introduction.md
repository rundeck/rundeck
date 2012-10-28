% Introduction
% Alex Honor; Greg Schueler
% November 20, 2010

## What is this manual about?

Welcome to the Rundeck user manual. This manual was written to help
administrators quickly become productive with the Rundeck server and tools. 

## What is Rundeck?

Rundeck is open source software that helps you automate ad-hoc and routine
procedures in data center or cloud environments. Rundeck provides a number 
of features that will alleviate time-consuming grunt work and make it easy for
you to scale up your scripting efforts.

Rundeck allows you to run tasks on any number of nodes from a web-based 
or command-line interface. Rundeck also includes other features that make 
it easy to scale up your scripting efforts including: access control, workflow 
building, scheduling, logging, and integration with external sources for node and 
option data.

Already itching to install it? Jump ahead to
[Installing Rundeck](getting-started.html#installing-rundeck).


### Who makes the Rundeck software?

Rundeck is developed on GitHub as a project called
[dtolabs/rundeck](https://github.com/dtolabs/rundeck).
Many ideas for Rundeck come from [DTO Solutions](http://www.dtosolutions.com)
consultants working in the field, however all are welcome to join the project
and contribute.

Rundeck is free software and is public under the [Apache Software License].

[Apache Software License]: http://www.apache.org/licenses/LICENSE-2.0.html

## Getting help

* Mailing list:
  [http://groups.google.com/group/rundeck-discuss](http://groups.google.com/group/rundeck-discuss)  
* IRC: irc://irc.freenode.net/rundeck
* Unix manual pages: Rundeck installation includes a set of Unix
  manual pages describing the shell tools. <code>man -k rundeck</code> 

## Rundeck from 30,000 feet

### Rundeck features

* distributed command execution
* pluggable execution system uses SSH by default
* multi-step workflows 
* job definition and on demand or scheduled runs
* graphical console for command and job execution
* role-based access control policy with support for LDAP/ActiveDirectory
* history and auditing logs
* open integration to external host inventory tools
* command line interface 
* Web API

### Rundeck in context

Rundeck is meant to compliment the tools you already use 
(including frameworks like Puppet, Chef, and Rightscale) and is geared
towards helping you automate actions across them. If you currently
manage your servers by running commands from the terminal or through
scripts that SSH commands in a loop, Rundeck is a more user friendly
alternative. Instead of managing node lists in a spreadsheet or wiki
page and then having to transcribe the list to where you execute commands,
Rundeck acts as a command and control portal that lets you execute
commands using features like node filtering and parallel execution.

Rundeck also works well for managing virtual servers, be they from a
cloud provider or from locally hosted virtualization software. The
node abstraction enabled by the Rundeck command dispatcher 
helps you cope with managing dynamic environments.

Many automation tasks cross the boundaries of tool sets. For example,
deploying software or maintaining an application often involves
using tools up and down the management tool chain. Rundeck has a simple
to use interface to create multi-step workflows that might call a
package manager, configuration management tool, system utilities, or your
own scripts. Rundeck is really meant to help glue tools together and
in return enable a push button interface you can hand off to others.

### Rundeck architecture

Rundeck is a server application you host on a system you designate 
a central administrative control point. Internally, Rundeck stores job
definitions and execution history in a relational database. Output
from command and job executions is saved on disk. 

Rundeck distributed command execution is performed using SSH. 
SSH connections are made using key-based authentication.
Rundeck server configuration includes settings to define the outbound
user allowed by the remote hosts. Remote machines
are not required to make SSH connections back to the server.

![Rundeck architecture](../figures/fig0001.png)

The Rundeck application itself is a Java-based webapp that runs in its
own embedded servlet container. The application provides both
graphical interface and network interfaces used by the Rundeck shell
tools. 

Access to the Rundeck application requires a login and
password. The default Rundeck installation uses a flat file user
directory containing a set of default logins. Logins are defined in
terms of a username and password as well as one or more user
groups. An alternative configuration to the flat file user directory,
is LDAP (e.g., ActiveDirectory). 
Users must also be authorized to perform actions like command and job
execution. This is controlled by an access control facility that reads
policy files defined by the Rundeck administrator. Privilege is
granted if a user's group membership meets the requirements of the policy.

Two installation methods are supported:

* RPM: The RPM is intended for managed installation and provides
  robust tools that integrate with your environment, man pages, shell
  tool set in your path, init.d startup and shutdown  
  
* Launcher: The launcher is intended for quick setup, to get you
  running right away.  Perfect for bootstrapping a project or trying
  a new feature.  

## Feedback

If you find problems with Rundeck, or if you have questions, remarks, or 
ideas about it, please send an email to the Rundeck mailing list,
[rundeck-discuss@groups.google.com](mailto:rundeck-discuss@groups.google.com). 

## What's next?

The remainder of the manual will give you a quick conceptual overview,
and take you through installation and setup. After you are set up, you
will learn about the distributed command dispatcher and how to use it
to run ad-hoc commands. From there, you will learn about Jobs,
defining multi-step procedures with Job workflows and how to
parameterize them with options.


