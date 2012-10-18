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

* `follow`
: Follow the output of the execution specified by ID

* `kill`
: kill the execution specified by ID

# OPTIONS

-h, \--help
:    Print usage message.

-e, \--eid
:    ID of the execution (kill or follow action only)

-p
:    Project name (list action only)

## follow mode

-q, \--quiet
:    Do not show output from the execution, but wait until it completes.

-r, \--progress
:    Show execution progress. For Jobs with a known average duration, '#' will indicate percentage complete, otherwise '.' will indicate continued progress.

-t, \--restart
:    If specified, all output from the Execution is retrieved from the beginning, rather than resuming from
     the current point in time.


# LIST ACTION #

This is the default action of the tool, so to list all running Executions for a project, simply use:

    rd-queue -p project

The output will display the number of executions, and their IDs and identifying names or descriptions, as well as a link to the Rundeck page to follow the output.

If there is only one project, the `-p` option can be left out.

*Example*

    rd-queue -p test
    Queue: 1 items
    [160] adhoc script job <http://localhost:8080/rundeck/execution/follow/160>

# FOLLOW ACTION #

This action allows you to specify the Execution ID of a currently running execution that you want to
follow the progress of.  The output of the execution will be retrieved from the current point in time 
onward and echoed locally to the console.

If `-t`/`--restart` is used, then all output of the execution is retrieved from the beginning.

If `-r`/`--progress` is used, then the output is not echoed, and a progress indicator bar is printed.

If `-q`/`--quiet`  is used, then no output is echoed, and the tool will wait until the execution completes, and 
exit with a non-zero exit status if the execution was not successful.

*Example*

    rd-queue follow -e 160
    Output from the job
    ...
    Final output
    [160] execution status: failed

*Example using --progress *

    rd-queue follow -e 160 --progress
    ####################.....
    [160] execution status: failed

# KILL ACTION #

This action allows you to specify the Execution ID of a currently running execution that you want to stop.

*Example*

    rd-queue kill -e 160
    rd-queue kill: success. [160] Job status: killed
  
# SEE ALSO

[`dispatch` (1)](dispatch.html), [`run` (1)](run.html).

The Rundeck source code and all documentation may be downloaded from
<https://github.com/dtolabs/rundeck/>.
