# Introduction 

## What is this guide about?

Welcome to the RunDeck user guide. This guide was written to help
administrators quickly become productive with the RunDeck server and tools. 

## What is RunDeck?

RunDeck is open source software that helps you automate ad-hoc and routine
procedures in data center or cloud environments. RunDeck provides a number 
of features that will alleviate time-consuming grunt work and make it easy for
you to scale up your scripting efforts.

### Who makes the RunDeck software?

RunDeck is developed on GitHub as a project called
[dtolabs/rundeck](https://github.com/dtolabs/rundeck).
Many ideas for RunDeck come from [DTO Solutions](http://www.dtosolutions.com)
consultants working in the field, however all are welcome to join the project
and contribute.

RunDeck is free software and is public under the [Apache Software License].

[Apache Software License]: http://www.apache.org/licenses/LICENSE-2.0.html

## Getting help

* Mailing list:
  [http://groups.google.com/group/rundeck-discuss](http://groups.google.com/group/rundeck-discuss)  
* IRC: irc://irc.freenode.net/rundeck
* Unix manual pages: RunDeck installation includes a set of Unix
  manual pages describing the shell tools. <code>man -k rundeck</code> 

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

RunDeck is meant to compliment the tools you already use 
(including frameworks like Puppet, Chef, and Rightscale) and is geared
towards helping you automate actions across them. If you currently
manage your servers by running commands from the terminal or through
scripts that SSH commands in a loop, RunDeck is a more user friendly
alternative. Instead of managing node lists in a spreadsheet or wiki
page and then having to transcribe the list to where you execute commands,
RunDeck acts as a command and control portal that lets you execute
commands using features like node filtering and parallel execution.

RunDeck also works well for managing virtual servers, be they from a
cloud provider or from locally hosted virtualization software. The
node abstraction enabled by the RunDeck command dispatcher 
helps you cope with managing dynamic environments.

Many automation tasks cross the boundaries of tool sets. For example,
deploying software or maintaining an application often involves
using tools up and down the management tool chain. RunDeck has a simple
to use interface to create multi-step workflows that might call a
package mangager, configuration mangement tool, system utilities, or your
own scripts. RunDeck is really meant to help glue tools together and
in return enable a push button interface you can hand off to others.

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
execution. This is controlled by an access control facility that reads
policy files defined by the RunDeck administrator. Privilege is
granted if a user's group membership meets the requirements of the policy.

Two installation methods are supported:

* RPM: The RPM is intended for managed installation and provides
  robust tools that integrate with your environment, man pages, shell
  tool set in your path, init.d startup and shutdown  
  
* Launcher: The launcher is intended for quick setup, to get you
  running right away.  Perfect for bootstrapping a project or trying
  a new feature.  

## What's next?

The remainder of the guide will give you a quick conceptual overview,
and take you through installation and setup. After you are set up, you
will learn about the distributed command dispatcher and how to use it
to run ad-hoc commands. From there, you will learn about Jobs,
defining multi-step procedures with Job workflows and how to
parameterize them with options.


