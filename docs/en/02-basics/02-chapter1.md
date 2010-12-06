% RUNDECK(1) RunDeck User Manuals | Version 1.0
% Alex Honor
% November 20, 2010

# RunDeck Basics 

This chapter covers the basics for using RunDeck. The chapter begins
by describing the RunDeck user interfaces, both its graphical and
command line. From there it will show you how to set up a project and
learn about command execution. You will learn more about using the
command dispatcher to control execution and finally, how to find and
use history.

## RunDeck Interfaces 

RunDeck provides two primary user interfaces:

* An HTML-based graphical console 
* A suite of shell tools

Both interfaces allow you to view resources, dispatch commands, as
well as, store and run jobs.

### Graphical Console 

To get started, go to the URL for your RunDeck server. 
Login to the web app with the credentials defined by the RunDeck user
directory configuration. 

#### Navigation

The RunDeck page header contains global navigation control to move
between browsing Resources, History and Jobs. It also has links to
logout and view the user's profile.

Resources

:    The Resources page displays the Node resources configured in your
     Project resource model. Like the Jobs and History pages, a filter
     control can be used to limit the listing to just the Node resources
     matching the filter criteria.

History

:    From the History page, one can view currently executing commands
     in the "Now Running" area or browse execution history. The execution
     history can be filtered based on user selected parameters. Once the
     filter has been set, the matching history is displayed. The current
     filter settings also configure an RSS link, found in the top right of
     the page.

Jobs

:    From the Jobs page, one can list, create and run Jobs. A
     configurable filter allows a user to limit the Job listing to those
     Jobs matching the filtering criteria. These filter settings can be
     saved to a Users profile. Only authorized jobs will be visible.

Admin

:    If your login belongs to the "admin" group and therefore granted
     "admin" privileges, a wrench icon will be displayed next to your login
     name. This page allows the admin to view group memberships for all
     users, as well as, edit their profile data.

Project menu

:    By default information about all projects is displayed. It is
     sometimes preferable to limit this information to just a particular
     project. The top navigation bar contains a menu to select the
     desired project. If only one project exists, the menu will
     automatically be defaulted.

### Shell Tools 

RunDeck includes a number of shell tools to dispatch commands, load
and run Job definitions and interact with the dispatcher queue. These
command tools are an alternative to functions accessible in the
graphical console.

`dispatch`
  ~ Execute ad hoc commands and scripts
`rd-queue`
  ~ Query the dispatcher for currently running Jobs and possibly kill them  
`rd-jobs`
  ~ List defined jobs as well as load them from text file definitions
`run`
  ~ Invoke the execution of a stored Job
`rd-project`
  ~ Setup a new RunDeck project
`rd-setup`
  ~ (Re-)configure an instance of RunDeck   
  
Consult the online manual pages for options and usage information.

## Project Setup 

A RunDeck *Project* provides a space to manage related management
activities. 

A Project can be set up either from the graphical console or using the
`rd-project` shell tool.

After logging into the graphical console, you will notice a Project
menu in the top navigation bar. If no projects exist, the menu will be
replaced by a single button. Create a new project by pressing that button
or choosing the "Create a Project" item from the menu. A dialog window
will open prompting you for a project name.

After typing in your project name, RunDeck initializes it and returns
you to the "Resources" view.

The `rd-project` shell tool can also be used to create a
project.

On the RunDeck server, execute the `rd-project` command and
specify a project name, here we use "examples":

    rd-project -a create -p examples

After running this command, you can login into the graphical console
and see the new project in the project menu.

The project setup process generates Project configuration in the server, and
a bootstrap resource model.

### Resource model

The initial resource model generated during project setup will contain
information just about the RunDeck server host and is useful just for
running local commands on the RunDeck server. 
You can browse the project resource model by going to the "Resources" page.

In the shell, you can list the Node resources in a resource
model using the shell tool, `dispatch`. 
Specify project name using the `-p project` option.

Here the `dispatch` command lists the registered server for
the "examples" project after the project setup. The `-v` gives
a verbose listing that includes more detail:

    $ dispatch -p examples -v	
     strongbad:
        hostname: strongbad
        os-arch: x86_64
        os-family: unix
        os-name: Mac OS X
        os-version: 10.6.2
        tags: []
       ---- Attributes ----

Node resources have standard properties, such as "hostname" but these
can be extended via attributes. One of the more useful properties
is the "tags" property. A *tag* is a text label that you give to the
Node, perhaps denoting a classification, a role the node plays in the
environment, or group membership. 

The output above shows the "strongbad" node currently has an empty
tags property: `tags: []`.

It is important to start thinking about node tagging for the nodes you manage
because you will use them later when specifying filtering
options to drive distributed command dispatch.

Each Project has its configuration located in its own directory
located in path like:
`$RDECK_BASE/projects/`_project_`/etc/project.properties`.

This configuration file contains two important properties for accessing and
storing resource model data:

* `project.resources.file`: File path to store resource
  model data (required).
* `project.resources.url`: URL to the resource model provider

You can configure RunDeck to retrieve and store resource model data
from any source, so long as it meets the RunDeck resource model
document requirement. Set the `project.resource.url` to the resource
model provider of your choice.

RunDeck reads the XML document retrieved from the `${project.resources.url}`
site and stores it in the path defined by `${project.resources.file}`.

Here's the XML document stored for the "examples" project that corresponds
to the output printed by the `dispatch -v` shown earlier:

    <project>
        <node name="strongbad" type="Node" 
          description="the RunDeck server host" tags="" 
          hostname="strongbad" 
          osArch="x86_64" osFamily="unix" osName="Mac OS X" osVersion="10.6.2"
          username="alexh" 
          editUrl="" remoteUrl=""/>
    </project>

Chances are you maintain information about your hosts within
another tool, perhaps Chef, Puppet, Nagios, Amazon EC2, RightScale or
even an in-house database. One of these tools might be
considered the authority of knowledge about the nodes
deployed in your network. Therefore, it is best to create an interface
to the authorative tool and expose it as RunDeck resource model provider. This
can be done as a simple CGI script that does a transformation from
the tool's format to the one RunDeck understands.

Of course, a rudimentary alternative is to maintain this information
as an XML document, storing it in a source repository that is
periodically exported to Rundeck. This method could be practical if
your host infrastructure rarely changes.

The "resource-v10(5)" manual contains reference information about the
RunDeck resources document content and structure.

Check the RunDeck web site for resource model providers. If you are
interested in creating your own, see the
[Resource model provider](#resource-model-provider-examples) section in the
[Examples](#rundeck-by-example) chapter.

## Command Execution

RunDeck supports two modes of execution: *ad-hoc commands* and *Job*.

An *ad-hoc command* is any system command or shell script executed
via the command dispatcher. Ad hoc commands can be executed via a
command line utility named `dispatch` or run from
the graphical console.

A *Job* specifies a sequence of one or more command invocations that
can be run once (i.e, is temporary) or named and stored for later use.
Stored jobs can be started via the shell tool, `run`, and
their progress checked with `rd-queue`.

### Dispatcher options

Dispatcher execution can be controlled by various types of options.

Execution control

:    Command execution can be controlled in various ways. Concurrency
     is controlled through threadcount. Execution can continue if specified
     to keepgoing

Include and exclude patterns

:    Filtering options specify include and exclude patterns to
     determine which nodes from the project resource model to distribute
     commands.

Keywords

:    Keywords are used within they include and exclude patterns. The
     "tags" keywords additionally can use a boolean operator to combine
     logical ORs and ANDs.

Option combination

:    All keywords can be combined by specifying the include and
     exclude options multiple times on the command line.
  
One can experiment querying the resource model in the graphical
console or with the `dispatch` tool.

#### Filtering nodes graphically  

Node resources are displayed in the Resources page. Setting the
project menu in the navigation bar will list just the Nodes
in that project's resource model.

Nodes can be filtered using include and exclude patterns by using
the Filter form. The form can be opened by pressing the "Filter" link.

When the form opens, you will see it divided into an Include section
where simple include expressions can be set, as well as, an "Extended
Filters..." link where exclude expressions can be made. 

After filling out the filter form, press "Filter" to generate a new
listing. Pressing "Clear" resets the form.

The Include and Exclude filters allow for filtering nodes based on the
following keywords: Name, Tags, Hostname, OS Name, OS Family, OS
Architecture, OS Version and Type.
  
#### Filtering nodes in the shell

`dispatch` uses the commandline options -I (include) and
-X (exclude) to specify which nodes to include and
exclude from the base set of nodes. You can specify a single value, a
list of values, or a regular expression as the argument to these
options.

*Examples*

List nodes  with OS name, Linux:

    dispatch -p examples -I os-name=Linux

List Linux nodes but exclude ones with names prefixed "web.":

    dispatch -p examples -I os-name=Linux -X "web.*"

List nodes that are tagged both "web" and "prod" :

    dispatch -p examples -I tags=web+prod

Here's an example that will execute the `apachectl restart`
command in 10 threads across all nodes tagged "web" and keepgoing in
case an error occurs :

    dispatch -p examples -I tags=web -K -C 10 -- sudo apachectl restart 

Consult the "rd-options(1)" manual page for the complete reference on
available dispatcher options.
  
### Ad-hoc commands 

Typically, an ad-hoc command is a shell script or system executable
that you run at an interactive terminal. Ad-hoc commands can be
executed via the `dispatch` shell command or a graphical
shell.

#### Shell tool command execution

Use `dispatch` to execute individual commands or shell script files.

Here `dispatch` is used to run the Unix `uptime` command to
print system status:

    $ dispatch -I os-family=unix -- uptime
    [ctier@centos54 dispatch][INFO]  10:34:54 up 46 min,  2 users,  load average: 0.00, 0.00, 0.00
    [alexh@strongbad dispatch][INFO] 10:34  up 2 days, 18:51, 2 users, load averages: 0.55 0.80 0.75
    [examples@ubuntu dispatch][INFO]  10:35:01 up 2 days, 18:40,  2 users,  load average: 0.00, 0.01, 0.00

Notice, the `dispatch` command prepends the message output
with a header that helps understand from where the output originates. The header
format includes the login and node where the `dispatch` execution
occurred.

Execute the Unix `whomi` command to see what user ID is
used by that Node to run dispatched commands:

    $ dispatch -I os-family=unix -- whoami
    [ctier@centos54 dispatch][INFO] ctier
    [alexh@strongbad dispatch][INFO] alexh
    [examples@ubuntu dispatch][INFO] examples

You can see that the resource model defines each Node to use a
different login to execute `dispatch` commands.  That
feature can be handy when Nodes serve different roles and therefore,
use different logins to manage processes. See the
`username` attribute in "resource-v10(5)" manual page.

The `dispatch` command can also execute shell
scripts. Here's a trivial script that generates a bit of system info:

    #!/bin/sh
    echo "info script"
    echo uptime=`uptime`
    echo whoami=`whoami`
    echo uname=`uname -a`

Use the -s option to specify the "info.sh" script file:

    $ dispatch -I os-family=unix -s info.sh
    
The `dispatch` command copies the "info.sh" script located
on the server to each "unix" Node and then executes it.

#### Graphical command shell execution

The RunDeck graphical console also provides the ability to execute
ad-hoc commands to a set of filtered Node resources.
The command prompt can accept any ad-hoc command string you might run
via an SSH command or via the `dispatch` shell tool.

But before running any commands, you need to select the project
containing the Nodes you wish to dispatch. Use the project
menu to select the desired project name. After the project has been
selected you will see a long horizontal textfield labeled
"Command". This is the RunDeck command prompt tool bar.

To use the command prompt, type the desired ad-hoc command string into
the textfield and press the "Run" button. The command will be
dispatched to all the Node resources currently listed below the
command prompt tool bar.

If the project selection menu was just chosen, then all Node resources
in that project resource model will be listed. You will most likely
want to limit the execution of your ad-hoc command to a subset of
these.

Use the filter control to refine the list of Nodes to target for your
ad-hoc command. Press the "Filter" link to open the filter control
form. Inside the filter form you will see an area to define an include
filter expression and a link to "Extended Filters..." where an exclusion
expression can also be defined. Many simple cases can use either a
regex pattern on Node name or a tag expression. Type in the desired
filter expression and press the "Filter" button to refine the Node
listing and redisplay the command prompt tool bar. 

Once you are satisifed with the Node listing, input the ad-hoc command
string, then press the "Run" button to begin execution. The browser
will be directed to a page where execution output can be followed. 

##### Following execution output

Command execution is displayed on a spearate page. This page provides
several views to read the output using different formats.

Tail Output

:   Displays output messages from the command execution as if you were
    running the Unix `tail -f` command on the output log file. 
    By default, only the last 20 lines of output is displayed but this
    can be expanded or reduced by pressing the "-" or "+" buttons. You
    can also type in an exact number into the textfield.

Annotated

:   The annotated mode displays the output messages in the order they
    are received but labels the each line with the Node from which the
    message originated. Through its additional controls each Node
    context can be expanded to show the output it produced, or
    completely collapsed to hide the textual detail.    

Node Output

:   Output messages are sorted into Node specific sections and are not
    interlaced. By default, the messages are collapsed but can be
    revealed by pressing the disclosure icon to the right. 

Also, notice the URL in the location bar of your browser. This URL can
be shared to others interested in the progress of execution. The URL
contains the execution ID (EID) and has a form like:

     http://rundeckserver/execution/follow/{EID}

After execution completes, the command will have a status: 

* Successful: No errors occurred during execution of the command
  across the filtered Node set
* Failed: One or more errors occurred. A list of Nodes that incurred
  an error is displayed. The page will also contain a link "Retry
  Failed Nodes..." in case you would like to retry the command.


You can download the entire output as a text file from this
page. Press the "Download" link to retrieve the file to your desk top.


### Controlling command execution

Parallel execution is managed using thread count via "-C" option. The
"-C" option specifies to the number of execution threads. Here's an
example that runs the uptime command across the Linux hosts with two
threads:

    dispatch -I os-name=Linux -C 2 -- uptime

The keepgoing and retry flags control when to exit incase an error
occurs. Use "-K/-R" flags. Here's an example script that checks if the
host has port 4440 in the listening state. If it does not, it will
exit with code 1.

    #!/bin/sh
    netstat -an | grep 4440 | grep -q LISTEN
    if [ "$?" != 0 ]; then
    echo "not listening on 4440"
    exit 1;
    fi
    echo  listening port=4440, host=`hostname`;

Commands or scripts that exit with a non-zero exit code will cause the
dispatch to fail unless the keepgoing flag is set.

    $ dispatch -I os-family=unix -s /tmp/listening.sh
    [alexh@strongbad dispatch][INFO] Connecting to centos54:22
    [alexh@strongbad dispatch][INFO] done.
    [ctier@centos54 dispatch][INFO] not listening on 4440
    error: Remote command failed with exit status 1

The script failed on centos54 and caused dispatch to error out immediately.

Running the command again, but this time with the "-K" keepgoing flag
will cause dispatch to continue and print on which nodes the script
failed:

    $ dispatch -K -I tags=web -s /tmp/listening.sh
    [alexh@strongbad dispatch][INFO] Connecting to centos54:22
    [alexh@strongbad dispatch][INFO] done.
    [ctier@centos54 dispatch][INFO] not listening on 4440
    [ctier@centos54 dispatch][ERROR] Failed execution for node: centos54: Remote command failed with exit status 1
    [alexh@strongbad dispatch][INFO] listening port=4440, host=strongbad
    [alexh@strongbad dispatch][INFO] Connecting to 172.16.167.211:22
    [alexh@strongbad dispatch][INFO] done.
    [examples@ubuntu dispatch][INFO] not listening on 4440
    [examples@ubuntu dispatch][ERROR] Failed execution for node: ubuntu: Remote command failed with exit status 1
    error: Execution failed on the following 2 nodes: [centos54, ubuntu]
    error: Execute this command to retry on the failed nodes:
	    dispatch -K -s /tmp/listening.sh -p examples -I
	    name=centos54,ubuntu
	
### Queuing commands to RunDeck

Commands or scripts executed on the command line by dispatch can also
be queued as temporary jobs in RunDeck by using the "-Q" option. The dispatch
-Q usage is equivalent to a "Run and Forget" action in the graphical console.

The script below is a long running check that will conduct a check periodically
waiting a set time between each pass. The script can be run with or without
arguments as the parameters are defaulted inside the script:

    $ cat ~/bin/checkagain.sh 
    #!/bin/bash
    iterations=$1 secs=$2 port=$3
    echo "port ${port:=4440} will be checked ${iterations:=30} times waiting ${secs:=5}s between each iteration" 
    i=0
    while [ $i -lt ${iterations} ]; do
      echo "iteration: #${i}"
      netstat -an | grep $port | grep LISTEN && exit 0
      echo ----
      sleep ${secs}
      i=$(($i+1))
    done
    echo "Not listening on $port after $i checks" ; exit 1

Running dispatch with the -Q option causes the execution to queue in
RunDeck and controlled as  temporary Job. The `-I centos54` limits
execution to just the "centos54" node:

    $ dispatch -Q -I centos54 -s ~/bin/checkagain.sh 
    Succeeded queueing workflow: Workflow:(threadcount:1){ [command( scriptfile: /Users/alexh/bin/checkagain.sh)] }
    Queued job ID: 5 <http://strongbad:4440/execution/follow/4>

To pass arguments to the script pass them after the "\--" (double
dash):

    $ iters=5 secs=60 port=4440
    $ dispatch -Q -I centos54 -s ~/bin/checkagain.sh -- $iters $secs $ports


### Tracking execution

Queued ad-hoc command and temporary or saved Job executions can be
tracked from the "History" page in the "Now Running" area at the top of
the page.

This page provides a listing of all running executions, when they
started, who started them and an approximation of their completion
progress.

Users with "workflow_kill" privilege, will also see a link to kill the
Job in case they want to stop it immediatly.

Execution can also be tracked using the `rd-queue` shell tool.

    $ rd-queue
    Queue: 1 items
    [5] workflow:
    Workflow:(threadcount:1){[command( scriptfile: /Users/alexh/bin/checkagain.sh)]
    } <http://strongbad:4440/execution/follow/5>

Running jobs can also be killed via the `rd-queue kill` command. The
rd-queue command includes the execution ID for each running
job. Specify execution ID using the "-e" option:

    $ ctl-queue kill -e 5
    ctl-queue kill: success. [5] Job status: killed


!!! TODO: Show now running page...

## History

History for queued ad-hoc commands, as well as, temporary and
saved Job executions  is stored by the RunDeck server. History data
can be filtered and viewed inside the "History" page of the graphical
console.

### Filtering event history

By default, the History page will list history for the last day's
executions. The page contains a filter control that can be used to
expand or limit the executions.

The filter form contains a number of fields to limit search:

* Within: Time range. Choices include 1 day, 1 week, 1 month or other
  (given a start after/before to ended after/before).
* Job Name: Job title name.
* Project: Project name. This may be set if the project menu was used.
* Resource: Name of project resource.
* User: User initiating action.
* Node: Node name.
* Tags: Event tag name.
* Report ID: Report identifier.
* Message: Message text.
* Result: Success or failure status.

After filling the form pressing the "Filter" button, the page will
display events matching the search.

Filters can be saved to a menu that makes repeating searches more
convenient. Click the "save this filter..." link to save the filter
configuration.

### Event view

History for each execution contains the command(s) executed,
dispatcher options, success status and a link to a file containing all
the output messages.

### RSS Link

An RSS icon provides a link to an RSS view of the events that match
the current filtering critera.

## Tips and Tricks 

### Saving filters

Each of the filter controls provides the means to save the current
filter configuration. Press the "save this filter..." link to give it
a name. Each saved filter is added to a menu you can access the next
time you want that filter configuration.

### Auto-Completion

If you use the Bash shell, RunDeck comes with a nice auto-completion
script you can enable. Add this to your `.bashrc` file:

    source $RDECK_BASE/etc/bash_completion.bash
  
Press the Tab key when you're writing a Git command, and it should
return a set of suggestions for you to pick from:

    $ dispatch <tab><tab>

## Summary

At this point, you can do basic RunDeck operations - setup a project,
define and query the project resource model, execute ad-hoc
commands, run and save Jobs and view history.

Next, we'll cover one of RunDeck's core features: Jobs.
