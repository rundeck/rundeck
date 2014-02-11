% run
% Alex Honor
% November 20, 2010

# NAME 

run - execute a stored Job on the Rundeck server

# SYNOPSIS

`run [-h] [-v] [-l level] [-F nodefilters] [-i id] [-j group/name][-- arguments]`

# DESCRIPTION 

The run command is used to start the execution of a Job defined in Rundeck. The Job is executed on the server, and the ID for the execution is displayed. This is equivalent to logging in to Rundeck and starting a Job within the web application GUI.

The Job can either be specified by ID (`-i`), or by name and optional group (`-j`). The Node filters used for the execution of the job can also be specified on the commandline, and will be used instead of any already defined for the Job. Arguments to the Execution can be specified after (`--`).

If the `-f`,`--follow` option is used, then the output will
be retrieved from the server as it is produced and echoed locally.

This can be combined with either `-q`,`--quiet` to show no output and
only wait until the execution finishes. Exit status will indicate
if the execution succeeded or failed.  If `-r`,`--progress` is used
instead, then progress of the execution is indicated periodically
by echoed '.' characters.

The `-F`/`--filter` option can be used to specify a node filter string. See [User Guide - Node Filters](../manual/node-filters.html).

# OPTIONS

`-h, --help`
: Print usage message.

`-v`
: Run verbosely.

`-l, --loglevel *LEVEL*`
: Run the command using the specified LEVEL. *LEVEL* can be `verbose`,
`info`, `warning`, `error`.

`-F, --filter *FILTER*`
: A node filter string

`-C *COUNT*`
: Threadcount, defaults to 1.

`-K`
: Keep going on node failure.

`-N` 
: Do not keep going on node failures.

`-j, --job *NAME*`
: Job job (group and name). Run a Job specified by Job name and
group. eg: 'group/name'.

`-i, --id *IDENTIFIER*`
: Run the Job with this IDENTIFIER

`-- *ARGUMENTS*`
: Pass the specified ARGUMENTS as options to the job

`-f, --follow`
: Follow queued execution output

`-r, --progress`
: In follow mode, print progress indicator chars

`-q, --quiet`
: In follow mode, do not show output from the execution, but wait until it completes.

# EXECUTION 

This tool requires a unique Job to be identified for execution. This is done either by using `-i id`, or `-j group/name`. The group is optional if only one Job with the name exists.

# ARGUMENTS

Arguments can be passed to the Job that is being executed after the `--` separator.  You must use this separator after all
of the tool options that you specify on the commandline.

Each argument that you pass should correspond to a defined Option for the Job.  The syntax for passing these options is:

    -optionname <value> -otheroption <value>

All Job Options in Rundeck require an argument value if specified.

## EXAMPLES

Run the Job that has ID `12`:

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ {.bash}
run -i 12
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 

Run the same job and pass arguments "name" and "color" corresponding to the Job's Options:

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ {.bash}
run -i 12 -- -name Bob -color blue
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 


Run the job named "Full" in the group "QA/Test"

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ {.bash}
run -g QA/Test -n Full
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 
    
Also runs the job "Full" in QA/Test group:

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ {.bash}
run -j QA/Test/Full
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 
    
If you try to run a job by name only, but it is not unique, you will
get an error message like this:

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ {.bash}
run -j testJob
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Error: Failed request to run a job: Server reported an error: No unique job matched the input: testJob, null. found (2)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 

Running a Job and specifying Node filters:

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ {.bash}    
run -F 'tags: dev !os-family: windows' -C 2 -K -j 'test/Job 1'
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 
Run a job by name, and follow the output until it finishes.

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ {.bash}
run -j QA/Test/Full --follow
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 

# SEE ALSO

* [`rd-queue`](rd-queue.html), [`rd-jobs`](rd-jobs.html)
* [User Guide - Node Filters](../manual/node-filters.html)

