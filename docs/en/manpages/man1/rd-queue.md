% rd-queue(1) | Version ${VERSION}
% Greg Schueler; Alex Honor
% July 7, 2011

# NAME

rd-queue - Lists and kills executions in the Rundeck queue

# SYNOPSIS

rd-queue [*action*] [-h] [-e id] [-p project]

# DESCRIPTION

The rd-queue command is used to query the Central Dispatcher for a list of currently running Executions, or to Kill a currently running execution.

The tool provides two actions:

* `list`
: list the currently running executions on the server (default action)

* `kill`
: kill the execution specified by ID

# OPTIONS

-h, \--help
:    Print usage message.

-e, \--eid
:    ID of the execution to kill

-p
:    Project name (list action only)


# LIST ACTION #

This is the default action of the tool, so to list all running Executions for a project, simply use:

    rd-queue -p project

The output will display the number of executions, and their IDs and identifying names or descriptions, as well as a link to the Rundeck page to follow the output.

If there is only one project, the `-p` option can be left out.

*Example*

    rd-queue -p test
    Queue: 1 items
    [160] adhoc script job <http://localhost:8080/rundeck/execution/follow/160>

# KILL ACTION #

This action allows you to specify the Execution ID of a currently running execution that you want to stop.

*Example*

    rd-queue kill -e 160
    rd-queue kill: success. [160] Job status: killed
  
# SEE ALSO

[`dispatch` (1)](dispatch.html), [`run` (1)](run.html).

The Rundeck source code and all documentation may be downloaded from
<https://github.com/dtolabs/rundeck/>.
