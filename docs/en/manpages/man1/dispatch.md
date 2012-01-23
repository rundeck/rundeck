% dispatch(1) | Version ${VERSION}
% Alex Honor
% November 20, 2010

# NAME

dispatch - execute commands across nodes via the rundeck dispatcher

# SYNOPSIS

dispatch [*options*]

# DESCRIPTION

The dispatch command is used to remotely execute ad-hoc shell commands
on a set of nodes which are specified by the filter options.

# OPTIONS

-h, \--help
: Print usage message.

-v
: Run verbosely.

-V 
: Turn on debug messages.

-q, \--quiet
: Show only error messages.

-z, \--terse
: Leave log messages unadorned

-N, \--nodesfile *FILE*
: Use specified file containing nodes resource model.

-C, \--threadcount *COUNT*
: Dispatch execution to Nodes using *COUNT* threads.

-K, \--keepgoing
: Keep going when an error occurs on multiple dispatch.

-I, \--nodes *FILTER*
: Include node parameter list.

-X, \--xnodes *FILTER*
: Exclude node parameter list.

\-filter-exclude-precedence *true|false*
: Set the exclusion filter to have precedence or not.

-p *NAME*
: Project name

\-- *COMMAND_STRING*
: Dispatch specified command string

-s, \--script *SCRIPT*
: Dispatch specified script file

-S, \--stdin
: Execute input read from *STDIN*

-Q, \--queue
: Queue this command to the dispatcher service and run it (deprecated)

-L, \--noqueue
: Execute the command locally not through the central dispatcher (experimental)

# COMMAND MODE #

Command mode occurs when the -p option is present (or there is only
one Project), and one (and only one) of the following options are
specified: \--, -s, or -S

## COMMAND STRING (\--) ##

The remote (or locally) shell command that is invoked is specified
after the "\--" on the command-line.
This string should begin with the command name and be followed by any
arguments you want to pass to it.

*Examples*

Execute the apachectl restart command across Nodes tagged "web":

    dispatch -I tags=web -- sudo apachectl restart

Execute apachectl status using the "keepgoing" flag across nodes that
have a hostname that begin with "web":

    dispatch -I hostname=web.* -K -- apachectl status

Run the locally installed update.sh script in three threads and
keepgoing if an error occurs:

    dispatch -I tags=dev -K -C 3 -- sh -c update.sh 

## SCRIPT (-s) ##

Sometimes it is preferable to save a sequence of command statements
into a file on the admin host and then execute that file across a
number of target hosts. The script specified via the -s option is a
script local to where the dispatch command is executed but that script
file is copied to the remote target machines and then executed.

*Examples*

Execute the "myscript.sh" shell script across the Nodes tagged "web":

    dispatch -I tags=web -s myscript.sh

Note: The script file is copied to a temporary directory on the target
machines (on unix it is /tmp and on Windows c:\windows\temp)

## STDIN (-S) ##

As an alternative to specifying the commands either as deferred
arguments after the double hyphen ("\--") or as a saved script ("-s
script"), dispatch can also read command input from stdin.

*Examples*

Execute the uname command across all Unix nodes

    echo "uname -a" | dispatch -I os-family=unix --stdin

Multi-line scripts are easier to write using a here document:

    dispatch --stdin <<END
      statement 1;
      statement 2;
      statement 3;
    END

Note: The input read from stdin is saved to a temporary file and then
content is called with the -s script mode described above to transfer and invoke the script.


# LISTING MODE #

dispatch will enter Listing Mode when no Command String is specified
on the command line.

In this mode, dispatch will output the list of available nodes.

*Example*

    $ dispatch
    daffy porky

When the -v (verbose) option is specified, the node listing will
include the details about the nodes that can be used for filtering:
hostname, os-arch, os-family, os-version, os-name, tags.

*Example*

    $ dispatch -v
        daffy:
           hostname: daffy.local
           os-arch: i386
           os-family: unix
           os-name: Mac OS X
           os-version: 10.5.2
           tags: [development]
        porky:
           hostname: porky
           os-arch: x86
           os-family: windows
           os-name: Windows XP
           os-version: 5.1
           tags: [testing]

When combined with the -I/-X Node Filtering options, you can easily
determine which nodes will be the target of any remotely executed
command prior to invoking it:

    dispatch -v -X os-family=unix
        porky:
           hostname: porky
           os-arch: x86
           os-family: windows
           os-name: Windows XP
           os-version: 5.1
           tags: [testing]

# ERROR CODE
The dispatch command will exit non zero if a command dispatch error
occurs.

0
: All commands executed successfully

1
: One or more commands failed

127
: Unknown error case
   
# SEE ALSO

[`rd-options` (1)](rd-options.html), [`rd-queue` (1)](rd-queue.html).

The Rundeck source code and all documentation may be downloaded from
<https://github.com/dtolabs/rundeck/>.
