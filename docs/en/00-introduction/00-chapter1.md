% RUNDECK(1) RunDeck User Manuals | Version 1.0
% Alex Honor
% November 20, 2010

# Introduction 

## What is this guide about?

Welcome to the RunDeck user guide. This guide was written to help
administrators quickly become productive with the RunDeck server and tools. 

RunDeck is open source software that helps administrators automate
ad-hoc and routine procedures.  RunDeck provides a number of features
that help you scale your scripting in a distributed environment for
multi-step procedures. RunDeck helps alleviate time-consuming grunt
work that comes from managing many systems in a dynamic environment.

### Who makes the RunDeck software?

RunDeck is hosted at GitHub as a project called
[dtolabs/rundeck](https://github.com/dtolabs/rundeck).
Dtolabs is a project where various open sources tools are
being developed by [DTO Solutions](http://www.dtosolutions.com)
engineers working in the field.

RunDeck is free software and is public under the [Apache Software License].

[Apache Software License]: http://www.apache.org/licenses/LICENSE-2.0.html

### Why is RunDeck open source?

We believe this kind of software project must be open source because
it is an interesting blending of technologies, philosophies and
therefore, RunDeck's ideas and code should be easily transferable.

### Why is it called RunDeck

The RunDeck name has meanings at a couple levels. The first level 
sees RunDeck as  a place (deck) to conduct your automation.
Another meaning suggests that groups of actions can be run at
once, like a workflow. This second meaning is more like a deck of
cards or to be really old fashioned, a deck of punch cards!

RunDeck also conjures up the idea of a tool for [run book automation]
but concentrates on being lightweight and focused on scripting rather
than serious development of workflow modules and state machines.

[run book automation]: http://en.wikipedia.org/wiki/Run_Book_Automation

## RunDeck from 30,000 feet

### RunDeck features

* distributed command execution via SSH
* multi-step workflows 
* job definition
* graphical console for command and job execution
* access control policy with support for LDAP/ActiveDirectory
* open integration to external host inventory tools
* command line interface 

### RunDeck in context

RunDeck is meant to compliment the tools you already use and is geared
towards helping you automate actions across them. If you currently
manage your servers by running commands from the terminal or through
scripts that SSH commands in a loop, RunDeck is a more user friendly
alternative. Instead of managing node lists in a spreadsheet or wiki
page and then having to transcribe the list to where you execute commands,
RunDeck acts as a command and control portal that lets you execute
commands using node filtering.

RunDeck also works well for managing virtual servers, be they from a
cloud provider or from locally hosted virtualization software. The
node abstraction enabled by the RunDeck command dispatcher (eg
leveraging filtering tags and using node information from external sources)
helps cope managing dynamic environments.

Many automation tasks cross the boundaries of tool sets. For example,
deploying software or maintaining an application often involves
using tools up down the management tool chain. RunDeck gives a simple
to use interface to create a multi-step workflow that might call a
package mangager (eg RPM), a configuration mangement (eg Puppet or
Chef), system utilities, and your own scripts. RunDeck is really meant
to help glue tools together and in return enable a push button interface.

### RunDeck architecture

RunDeck is a server application you host on a system you designate 
a central administrative control point. Internally, RunDeck stores job
definitions and execution history in a relational database. Output
from command and job executions is saved on disk. 

RunDeck distributed command execution is performed using SSH. Either
key-based or username/password authentication is supported. The
RunDeck server configuration includes settings to define the outbound
user that must also be allowed by the remote hosts. Remote machines
are not required to make SSH connections back to the server.

The RunDeck application itself is a Java-based webapp that runs in its
own embedded servlet container. The application provides both
graphical interface and network interfaces used by the RunDeck shell
tools. 

Access to the RunDeck application requires a login and
password. The default RunDeck installation uses a flat file containing
a set of default logins. Logins are defined in terms of a username and
password as well as one or more user groups. An alternative
configuration to the flat file user directory, is LDAP (or
ActiveDirectory). 
Users must also be authorized to perform actions like command and job
execution. This is controled by an access control facility that reads
policy files defined on disk. Privilege is granted if a user's group
membership meets the requirements of the policy.

Two installation methods are supported:

* RPM: The RPM is intended for managed installation and provides
  robust tools that integrate with your environment, man pages, shell
  tool set in your path, init.d startup and shutdown  
  
* Launcher: The launcher is intended for quick setup, to get you
  running right away.  Perfect for bootstrapping a project or trying
  a new feature.  

## Getting help

* Mailing list:
  [http://groups.google.com/group/rundeck-discuss](http://groups.google.com/group/rundeck-discuss)  
* IRC: irc://irc.freenode.net/rundeck
* Unix manual pages: RunDeck installation includes a set of Unix
  manual pages describing the shell tools. <code>man -k rundeck</code> 

  
## What's next?

The remainder of the guide will give you a quick conceptual overview,
and take you through installation and setup. After you are set up, you
will learn about the distributed command dispatcher and how to use it
to run ad-hoc commands. From there, you will learn about Jobs,
defining multi-step procedures with Job workflows and how to
parameterize them with options.


