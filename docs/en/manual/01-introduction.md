% Introduction
% Alex Honor; Greg Schueler
% November 20, 2010

## What is this manual about?

Welcome to the Rundeck user manual. This manual was written to help
you quickly become productive with the Rundeck server and tools. 

## What is Rundeck?

Rundeck is open source software that helps you automate routine operational
procedures in data center or cloud environments. Rundeck provides a number 
of features that will alleviate time-consuming grunt work and make it easy for
you to scale up your automation efforts and create self service for others. 
Teams can collaborate to share how processes are automated while
others are given trust to view operational activity or execute tasks.

Rundeck allows you to run tasks on any number of nodes from a web-based 
or command-line interface. Rundeck also includes other features that make 
it easy to scale up your automation efforts including: access control, workflow 
building, scheduling, logging, and integration with external sources for node and 
option data.

Already itching to install it? Jump ahead to
[Installing Rundeck](getting-started.html#installing-rundeck).


### Who makes the Rundeck software?

Rundeck is developed on GitHub as a project called
[rundeck](https://github.com/dtolabs/rundeck)
by [SimplifyOps](http://simplifyops.com) and the Rundeck community.
All new users are welcomed to participate in the project and contribute.
Please vote on feature ideas on the [Rundeck Trello Board](https://trello.com/b/sn3g9nOr/rundeck-development).

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

* Web API
* distributed command execution
* pluggable execution system (SSH by default)
* multi-step workflows 
* job execution with on demand or scheduled runs
* graphical web console for command and job execution
* role-based access control policy with support for LDAP/ActiveDirectory
* history and auditing logs
* open integration to external host inventory tools
* command line interface tools


### Rundeck in context

Rundeck is meant to compliment the tools you already use 
(including frameworks like Puppet, Chef, and Jenkins, Cloud, VM) and is geared
towards helping you automate actions across them. If you currently
manage your servers by running commands from the terminal or through
scripts, Rundeck is a more user friendly
alternative. Instead of managing node lists in a spreadsheet or wiki
page and then having to transcribe the list to where you execute commands,
Rundeck acts as a command and control portal that lets you execute
commands using features like node filtering and parallel execution.

Rundeck also works well for managing virtual servers, be they from a
cloud provider or from locally hosted virtualization software. The
node abstraction enabled by the Rundeck command dispatcher 
helps you cope with managing dynamic environments.

Many automation tasks cross the tool boundaries. For example,
deploying software or maintaining an application often involves
using tools up and down the management tool chain. Rundeck has a simple
to use interface to create multi-step workflows that might call a
package manager, configuration management tool, system utilities, or your
own scripts. Rundeck is really meant to help automate tasks across
tools and  in return enable a push button interface you can hand off to others.

### Rundeck architecture

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

## Feedback

If you find problems with Rundeck, or if you have questions, remarks, or 
ideas about it, please send an email to the Rundeck mailing list,
[rundeck-discuss@groups.google.com](mailto:rundeck-discuss@groups.google.com). 

## What's next?

The remainder of the manual will give you a quick conceptual overview,
and take you through installation and setup. After you are set up, you
will learn about the Rundeck interfaces, how to navigate to Jobs,
Nodes and History. How to use the command dispatcher
to run ad-hoc commands. From there, you will learn more about Jobs,
defining multi-step procedures with Job workflows and how to
parameterize them with options.


