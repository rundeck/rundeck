% Getting Started
% Alex Honor; Greg Schueler
% November 20, 2010

This chapter helps new users getting started with Rundeck. We will begin
by explaining the basics, covering essential Rundeck concepts and
terminology and then move on to installation and finally, setup.
At the end of this chapter you should understand what Rundeck is, how
you should use it and you should be all setup to do so.

## Rundeck Basics

Several fundamental concepts underly and drive the development of the
Rundeck system. If you are a new user, knowing about them will
help you use and integrate Rundeck into your environment.

### Plugins

Most of what Rundeck does is via one of its plugins. Plugins exist
to execute commands on nodes, perform a step in a workflow, 
send a notification about job status, gather
information about the hosts in your network, copy a file to a remote
server, store and stream logs, or talk to a user directory.

### Node execution

Rundeck supports a notion called *node execution* wherein a
user specifies a "command" and a "node filter" to match a list of nodes.
Rundeck uses the filter to produce a list of matching nodes and
then dispatches the command to them.
A *node executor* is a Rundeck plugin that implements how to
communicate with the Node and how to invoke the command action.

The command executes in a data context that contains information 
about the Node resource, job, and job option data. Your command
can use this data and thus avoid hard coding node or environment
specific values.


### Jobs

*Jobs* encapsulate a sequence of steps, node filter and job options
to provide a single executable action. Jobs can be given a unique
ID, a name and group to organize them to run any time 
or they may be scheduled to run periodically.

Jobs take a number of parameters to control execution, including
what to do when an error occurs in one of the steps or
how many threads to execute actions across a list of nodes.

Jobs can be composed out of other jobs. Each job can be a step
in another job sequence. 

### Resource model

The *resource model* is a representation of hosts deployed in your
network. A _Node_  is a resource that is either a physical or virtual instance
of a network accessible host.

Nodes have a number of basic attributes but these attributes can be
extended to include arbitrary named key/value pairs. Rundeck 
node filters let you match Nodes based on a pattern like "tags" or
a node attribute.

A *resource model source* is a Rundeck plugin that produces a set of
node definitions for a project.
You can configure Rundeck to retrieve and store resource model data
from multiple sources, and Rundeck defines several resource model
document formats to facilitate the transfer of this information. 

Resource Model data sources can be local files on disk, or remotely
accessible services. A *URL resource model source* is an external service
accessible via the HTTP GET method that returns data in one of the supported
resource document formats.

Rundeck currently supports XML and YAML document formats. 
See [Resource Model Document formats](rundeck-basics.html#resource-model-document-formats))
to learn how to introduce your own format.

Each project can be configured to have multiple sources of Resource Model data. 
See [Resource Model Sources](plugins.html#resource-model-sources).

### Authorization

Rundeck enforces an *access control policy* that grants certain
privileges to groups of users.
Every action executed through the Rundeck node executor must meet
the requirements of an access control policy definition. 

Since Rundeck respects the policy definition, you can define role-based
authorization to restrict some users to only a subset of actions. This
enables a self-service type interface, where some users have
access to only a limited set of executable actions.

See: [Authorization](../administration/authorization.html).

### Project

A *project* is a place to separate management activity.
All Rundeck activities occur within the context of a project.
Each project has its own resource model and Job store.

Multiple projects can be maintained on the same Rundeck server.
Projects are independent from one another, so you can use them to
organize unrelated systems within a single Rundeck
installation. This can be useful for managing different infrastructures.

### API

You can also use the Web API to interface with all aspects of node
and Job execution. (See the [Rundeck API](../api/index.html).)

## Installing Rundeck

For more detailed install instructions, see the [Administration - Installation](../administration/installation.html) chapter.

The simplest way to try Rundeck is by using the Launcher jar.  
Simply download it, and place it into a directory that will be the `RDECK_BASE` base directory.

Start the Rundeck server by running the jar using java:

    java -jar rundeck-launcher-1.6.0.jar

This will extract the contents into the current working directory and start the service.
You can Ctl-C the process and then start and stop it using the `rundeckd` script
(see [startup and shutdown](../administration/startup-and-shutdown.html)).

## Upgrading Rundeck

If you are upgrading Rundeck from a previous version, 
please read the [Rundeck Upgrade Guide](../upgrading/index.html).

## First-Time Setup

### Logins 

Rundeck supports a number of user directory configurations. By
default, the installation uses a file based directory, but connectivity to
LDAP is also available. 
See [Administration - Authentication](../administration/authentication.html).

The Rundeck installation process will have defined a set of temporary
logins useful during the getting started phase.

* `user`: Has access to run commands and jobs but unable to modify job
  definitions. Password: "user"
* `admin`: Belongs to the "admin" group and is automatically granted
  the "admin" and "user" role privileges. Password: "admin"
  
### Group membership

If you installed Rundeck using the RPM installation method, it will
have created a unix group called "rundeck".

    $ groups rundeck
    rundeck : rundeck

It also made several log files writable to members of the "rundeck" group.

    $ ls -l /var/log/rundeck/command.log
    -rw-rw-r-- 1 rundeck rundeck 588 Dec  2 11:24 /var/log/rundeck/command.log

If you want to use the Rundeck shell tools, be sure to add that group
to the necessary user accounts.

Rundeck shell tool users that do not belong to group, rundeck, will
get error messages like so:

    $ rd-jobs
    log4j:ERROR setFile(null,true) call failed. java.io.FileNotFoundException: /var/log/rundeck/command.log (Permission denied)

Consult the [usermod] command to modify a user account.

[usermod]: http://linux.die.net/man/8/usermod

## Summary 

You should now have a basic understanding of Rundeck. You
should also have a working version of Rundeck on your system
and login access. It is now time to learn some Rundeck basics.

  
