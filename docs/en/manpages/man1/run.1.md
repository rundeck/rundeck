% RUN(1) RunDeck User Manuals | Version @VERSION@
% Alex Honor
% November 20, 2010

# NAME 

run - execute a stored Job on the RunDeck server

# SYNOPSIS

run [-h] [-v] [-l level] [nodefilters] [-i id] [-j group/name][\-- arguments]

# DESCRIPTION 

The run command is used to start the execution of a Job defined in RunDeck. The Job is executed on the server, and the ID for the execution is displayed. This is equivalent to logging in to RunDeck and starting a Job within the web application GUI.

The Job can either be specified by ID (-i), or by name and optional group (-j). The Node filters used for the execution of the job can also be specified on the commandline, and will be used instead of any already defined for the Job. Arguments to the Execution can be specfied after (\--).


# OPTIONS

-h, \--help
: Print usage message.

-v
: Run verbosely.

-l, \--loglevel *LEVEL*
: Run the command using the specified LEVEL. *LEVEL can be `verbose`,
`info`, `warning`, `error`.

-I *FILTER*
: Include node filter.

-X *FILTER*
: Exclude node filter.

-C *COUNT*
: Threadcount, defaults to 1.

-K
: Keep going on node failure.

-N 
: Do not keep going on node failures.

-j, \--job *NAME*
: Job job (group and name). Run a Job specified by Job name and
group. eg: 'Group/Job'.

-i, \--id *IDENTIFIER*
: Run the Job with this IDENTIFIER

\-- *ARGUMENTS*
: Pass the specified ARGUMENTS as options to the job

# EXECUTION 

This tool requires a unique Job to be identified for execution. This is done either by using `-i id`, or `-j group/name`. The group is optional if only one Job with the name exists.

## EXAMPLES

Run the Job that has ID `12`:

    run -i 12

Run the job named "Full" in the group "QA/Test"

    run -g QA/Test -n Full
    
Also runs the job "Full" in QA/Test group:

    run -j QA/Test/Full
    
If you try to run a job by name only, but it is not unique, you will
get an error message like this:

    run -j testJob
    Error: Failed request to run a job: Server reported an error: No unique job matched the input: testJob, null. found (2)

Running a Job and specifying Node filters:
    
    run -I tags=dev -X os-family=windows -C 2 -K -j 'test/Job 1'
 

# SEE ALSO

`rd-queue` (1), `rd-jobs` (1).

The RunDeck source code and all documentation may be downloaded from
<https://github.com/dtolabs/rundeck/>.

