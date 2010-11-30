% RD-QUEUE(1) RunDeck User Manuals | Version 1.0
% Greg Schueler; Alex Honor
% November 20, 2010

# NAME

rd-queue - Lists and kills executions in the RunDeck queue

# SYNOPSIS

rd-queue [*action*] [-h] [-e]

# DESCRIPTION

The rd-queue command is used to query the Central Dispatcher for a list of currently running Executions, or to Kill a currently running execution.

The tool provides two actions:

* `list`
: list the currently running executions on the server (default action)

* `kill`
: kill the execution specified by ID

# OPTIONS

-h, \--help
: Print usage message.

-e, \--eid
: ID of the execution to kill


# LIST ACTION #

This is the default action of the tool, so to list all running Executions, simply use:

    rd-queue

The output will display the number of executions, and their IDs and identifying names or descriptions, as well as a link to the RunDeck page to follow the output.

*Example*

    rd-queue 
    Queue: 1 items
    [160] adhoc script job <http://localhost:8080/rundeck/execution/follow/160>

# KILL ACTION #

This action allows you to specify the Execution ID of a currently running execution that you want to stop.

*Example*

    rd-queue kill -e 160
    rd-queue kill: success. [160] Job status: killed
  
# SEE ALSO

`dispatch` (1), `run` (1).

The RunDeck source code and all documentation may be downloaded from
<https://github.com/dtolabs/rundeck/>.
