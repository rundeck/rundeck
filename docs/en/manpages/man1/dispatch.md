% dispatch
% Alex Honor
% November 20, 2010

# NAME

dispatch - execute commands across nodes via the rundeck dispatcher

# SYNOPSIS

`dispatch [*options*]`

# DESCRIPTION

The dispatch command is used to remotely execute ad-hoc shell commands
on a set of nodes which are specified by the filter options.

# OPTIONS

`-h, --help`
: Print usage message.

`-v`
: Run verbosely.

`-V`
: Turn on debug messages.

`-q, --quiet`
: Show only error messages.

`-C, --threadcount *COUNT*`
: Dispatch execution to Nodes using *COUNT* threads.

`-K, --keepgoing`
: Keep going when an error occurs on multiple dispatch.

`-F, --filter *FILTER*`
: A node filter string

`-filter-exclude-precedence *true|false*`
: Set the exclusion filter to have precedence or not.

`-p *NAME*`
: Project name

`-- *COMMAND_STRING*`
: Dispatch specified command string

`-s, --script *SCRIPT*`
: Dispatch specified script file

`-u, --url *URL*`
: Download a URL and dispatch it as a script

`-S, --stdin`
: Execute input read from *STDIN*

`-f, --follow`
: Follow queued execution output

`-r, --progress`
: In follow mode, print progress indicator chars

# COMMAND MODE #

Command mode occurs when the `-p` option is present (or there is only
one Project), and one (and only one) of the following options are
specified: `--, -s, or -S`

The default behavior is to invoke the "queue" mode,
which will send the desired command to the Rundeck server for execution, 
and return the ID of the queued Execution.

If "follow" option is used (`-f`/`--follow`), then the output will
be retrieved from the server as it is produced and echoed locally.

This can be combined with either `-q`/`--quiet` to show no output and
only wait until the execution finishes. Exit status will indicate
if the execution succeeded or failed.  If `-r`/`--progress` is used
instead, then progress of the execution is indicated periodically
by echoed '.' characters.

The `-F`/`--filter` option can be used to specify a node filter string. See [User Guide - Node Filters](../manual/node-filters.html).

## COMMAND STRING (`--`) ##

The remote (or locally) shell command that is invoked is specified
after the `--` on the command-line.
This string should begin with the command name and be followed by any
arguments you want to pass to it.

*Examples*

Execute the apachectl restart command across Nodes tagged "web":

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ {.bash}
dispatch -F 'tags: web' -- sudo apachectl restart
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Execute apachectl status using the "keepgoing" flag across nodes that
have a hostname that begin with "web":

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ {.bash}
dispatch -F 'hostname: web.*' -K -- apachectl status
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Run the locally installed update.sh script in three threads and
keepgoing if an error occurs:

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ {.bash}
dispatch -F 'tags: dev' -K -C 3 -- sh -c update.sh 
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

## SCRIPT (`-s`) ##

Sometimes it is preferable to save a sequence of command statements
into a file on the admin host and then execute that file across a
number of target hosts. The script specified via the -s option is a
script local to where the dispatch command is executed but that script
file is copied to the remote target machines and then executed.

*Examples*

Execute the "myscript.sh" shell script across the Nodes tagged "web":

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ {.bash}
dispatch -F 'tags: web' -s myscript.sh
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Note: The script file is copied to a temporary directory on the target
machines (on unix it is /tmp and on Windows c:\windows\temp)

## URL (`-u`) ##

Downloads a URL and then dispatches it for execution as a script on the
target nodes.

*Examples*

Execute a shell script available at a URL across the Nodes tagged "web":

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ {.bash}
dispatch -F 'tags: dev' -u http://ops.example.com/scripts/myscript.sh
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 

Note: The script file is copied to a temporary directory on the target
machines (on unix it is /tmp and on Windows c:\windows\temp)

The URL can contain property references expanded in the execution
context.

## STDIN (`-S`) ##

As an alternative to specifying the commands either as deferred
arguments after the double hyphen ("\--") or as a saved script ("-s
script"), dispatch can also read command input from stdin.

*Examples*

Execute the uname command across all Unix nodes

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ {.bash}
echo "uname -a" | dispatch -F 'os-family: unix' --stdin
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Multi-line scripts are easier to write using a here document:

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ {.bash}
dispatch --stdin <<END
  statement 1;
  statement 2;
  statement 3;
END
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 

Note: The input read from stdin is saved to a temporary file and then
content is called with the -s script mode described above to transfer and invoke the script.

# QUEUED EXECUTION #

The execution will be sent to the Rundeck server, and the execution ID
echoed.

# LISTING MODE #

dispatch will enter Listing Mode when no Command String is specified
on the command line.

In this mode, dispatch will output the list of available nodes.

*Example*

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ {.bash}
dispatch
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~    
daffy porky
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 

When the -v (verbose) option is specified, the node listing will
include the details about the nodes that can be used for filtering:
hostname, os-arch, os-family, os-version, os-name, tags.

*Example*

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ {.bash}
dispatch -v
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~    
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
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 

When combined with the -F Node Filter option, you can easily
determine which nodes will be the target of any remotely executed
command prior to invoking it:

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ {.bash}
dispatch -v -F '!os-family: unix'
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~    

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
porky:
   hostname: porky
   os-arch: x86
   os-family: windows
   os-name: Windows XP
   os-version: 5.1
   tags: [testing]
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 

# Examples


Typically, an ad-hoc command is a shell script or system executable
that you run at an interactive terminal. Ad-hoc commands can be
executed via the `dispatch` shell command or from the Nodes page
in the GUI.

#### Shell tool command execution

Use `dispatch` to execute individual commands or shell script files.

Here `dispatch` is used to run the Unix `uptime` command to
print system status:

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ {.bash}
dispatch -F 'os-family: unix' -- uptime
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Succeeded queueing Workflow execution: Workflow:(threadcount:1){ [command( exec: uptime)] }
Queued job ID: 7 <http://strongbad:4440/execution/follow/7>
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 

The ``uptime`` command is queued and executed. The output can be followed by
going to the URL returned in the output (eg, http://strongbad:4440/execution/follow/7). 

Sometimes it is desirable to execute the command
and follow the output from the console. Use the `-f` flag to echo the output as the command is executed by the server:

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ {.bash}
dispatch -F 'os-family: unix' -f -- uptime
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~    
[demo@centos54 dispatch][INFO]  10:34:54 up 46 min,  2 users,  load average: 0.00, 0.00, 0.00
[alexh@strongbad dispatch][INFO] 10:34  up 2 days, 18:51, 2 users, load averages: 0.55 0.80 0.75
[examples@ubuntu dispatch][INFO]  10:35:01 up 2 days, 18:40,  2 users,  load average: 0.00, 0.01, 0.00
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 

Notice, the `dispatch` command prepends the message output
with a header that helps understand from where the output originates. The header
format includes the login and node where the `dispatch` execution
occurred.

Execute the Unix `whoami` command to see what user ID is
used by that Node to run dispatched commands:

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ {.bash}
dispatch -F 'os-family: unix' -f -- whoami
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~    
[demo@centos54 dispatch][INFO] demo
[alexh@strongbad dispatch][INFO] alexh
[examples@ubuntu dispatch][INFO] examples
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 

You can see that the resource model defines each Node to use a
different login to execute `dispatch` commands.  That
feature can be handy when Nodes serve different roles and therefore,
use different logins to manage processes. See the
`username` attribute in 
[resource-XML](../man5/resource-xml.html) or 
[resource-YAML](../man5/resource-yaml.html) manual page.

The `dispatch` command can also execute shell
scripts. Here's a trivial script that generates a bit of system info:

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ {.bash}
#!/bin/sh
echo "info script"
echo uptime=`uptime`
echo whoami=`whoami`
echo uname=`uname -a`
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 

Use the -s option to specify the "info.sh" script file:

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ {.bash}
dispatch -F 'os-family: unix' -s info.sh
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 
    
The `dispatch` command copies the "info.sh" script located
on the server to each "unix" Node and then executes it.




### Controlling command execution

Parallel execution is managed using thread count via "-C" option. The
"-C" option specifies the number of execution threads. Here's an
example that runs the uptime command across the Linux hosts with two
threads:

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ {.bash}
dispatch -F 'os-name: Linux' -C 2 -- uptime
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 

The keepgoing and retry flags control when to exit incase an error
occurs. Use "-K/-R" flags. Here's an example script that checks if the
host has port 4440 in the listening state. If it does not, it will
exit with code 1.

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ {.bash}
#!/bin/bash
netstat -an | grep 4440 | grep -q LISTEN
if [ "$?" != 0 ]
then
  echo "not listening on 4440"
  exit 1
fi
echo "listening port=4440, host=$(hostname)";
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 

Commands or scripts that exit with a non-zero exit code will cause the
dispatch to fail unless the keepgoing flag is set.

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ {.bash}
dispatch -F 'os-family: unix' -s /tmp/listening.sh -f
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~    
[alexh@strongbad dispatch][INFO] Connecting to centos54:22
[alexh@strongbad dispatch][INFO] done.
[demo@centos54 dispatch][INFO] not listening on 4440
error: Remote command failed with exit status 1
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 

The script failed on centos54 and caused dispatch to error out immediately.

Running the command again, but this time with the "-K" keepgoing flag
will cause dispatch to continue and print on which nodes the script
failed:

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ {.bash}
dispatch -f -K -F 'tags: web' -s /tmp/listening.sh
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~    
[alexh@strongbad dispatch][INFO] Connecting to centos54:22
[alexh@strongbad dispatch][INFO] done.
[demo@centos54 dispatch][INFO] not listening on 4440
[demo@centos54 dispatch][ERROR] Failed execution for node: centos54: Remote command failed with exit status 1
[alexh@strongbad dispatch][INFO] listening port=4440, host=strongbad
[alexh@strongbad dispatch][INFO] Connecting to 172.16.167.211:22
[alexh@strongbad dispatch][INFO] done.
[examples@ubuntu dispatch][INFO] not listening on 4440
[examples@ubuntu dispatch][ERROR] Failed execution for node: ubuntu: Remote command failed with exit status 1
error: Execution failed on the following 2 nodes: [centos54, ubuntu]
error: Execute this command to retry on the failed nodes:
  dispatch -K -s /tmp/listening.sh -p examples -F  'centos54,ubuntu'
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  
### Queuing commands 

The script below is a long running check that will conduct a check periodically
waiting a set time between each pass. The script can be run with or without
arguments as the parameters are defaulted inside the script:

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ {.bash}
$ cat ~/bin/checkagain.sh 
#!/bin/bash
iterations=$1 secs=$2 port=$3
echo "port ${port:=4440} will be checked ${iterations:=30} times waiting ${secs:=5}s between each iteration" 
i=0
while [ $i -lt ${iterations} ]
do
  echo "iteration: #${i}"
  netstat -an | grep $port | grep LISTEN && exit 0
  echo ----
  sleep ${secs}
  i=$(($i+1))
done
echo "Not listening on $port after $i checks" ; exit 1
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Running `dispatch` causes the execution to queue in
Rundeck and controlled as  temporary Job. The `-F centos54` limits
execution to just the "centos54" node:

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ {.bash}
dispatch -F centos54 -s ~/bin/checkagain.sh 
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~    
Succeeded queueing workflow: Workflow:(threadcount:1){ [command( scriptfile: /Users/alexh/bin/checkagain.sh)] }
Queued job ID: 5 <http://strongbad:4440/execution/follow/4>
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

To pass arguments to the script pass them after the `--` (double
dash):

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ {.bash}
iters=5 secs=60 port=4440

dispatch -F centos54 -s ~/bin/checkagain.sh -- $iters $secs $ports
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 



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

* [`rd-queue`](rd-queue.html).
* [User Guide - Node Filters](../manual/node-filters.html)
