% Introduction
% Alex Honor; Greg Schueler
% November 20, 2010

## What is this guide about?

Welcome to the Rundeck user guide. This guide was written to help
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
[Installing Rundeck](getting-started.html#download-and-installation).


### Who makes Rundeck?

Rundeck is developed on GitHub as a project called
[rundeck](https://github.com/rundeck/rundeck)
by [SimplifyOps](http://simplifyops.com) and the Rundeck community.
All new users are welcomed to participate in the project and contribute.
Please vote on feature ideas on the [Rundeck Trello Board](https://trello.com/b/sn3g9nOr/rundeck-development).

Rundeck is free software and is public under the [Apache Software License].

[Apache Software License]: http://www.apache.org/licenses/LICENSE-2.0.html



## Rundeck features

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


## Rundeck in context

Rundeck is meant to complement the tools you already use 
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

