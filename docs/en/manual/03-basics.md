% Rundeck Basics 
% Alex Honor; Greg Schueler
% November 20, 2010

This chapter covers the basics for using Rundeck. The chapter begins
by describing the Rundeck user interfaces, both its graphical and
command line. From there it will show you how to set up a project and
learn about command execution. You will learn more about using the
command dispatcher to control execution and finally, how to find and
use history.

## Rundeck Interfaces 

Rundeck provides two primary user interfaces:

* An HTML-based graphical console 
* A suite of shell tools

Both interfaces allow you to view resources, dispatch commands, as
well as, store and run jobs.

In addition Rundeck provides a Web API which can be used to interact with
the server programattically. See [Rundeck API](../api/index.html).

### Graphical Console 

To get started, go to the URL for your Rundeck server. 
Login to the web app with the credentials defined by the Rundeck user
directory configuration. 

The default port for the web interface is `4440`, so try this URL:

<http://localhost:4440>

You will be shown the Login page.  The default username and password are `admin`/`admin`, so enter those in the fields.

#### Navigation

The Rundeck page header contains global navigation control to move
between tabbed pages: Run,  Jobs and History. It also has links to
logout, view your user profile and a link to this online help.

![Top navigation bar](../figures/fig0201.png)

Run

:    The Run page is used to execute ad hoc commands. It
     displays filtered Node resources configured in your
     Project resource model. A filter  control can be used to 
     limit the listing to just the Node resources
     matching the filter criteria.

Jobs

:    From the Jobs page, one can list, create and run Jobs. A
     configurable filter allows a user to limit the Job listing to those
     Jobs matching the filtering criteria. These filter settings can be
     saved to a Users profile. Only authorized jobs will be visible.

History

:    From the History page, one can view currently executing commands
     in the "Now Running" area or browse execution history. The execution
     history can be filtered based on user selected parameters. Once the
     filter has been set, the matching history is displayed. The current
     filter settings also configure an RSS link, found in the top right of
     the page.

Project menu

:    The top navigation bar contains a menu to select the
     desired project. If only one project exists, the menu will
     automatically be defaulted.
     
Admin

:    If your login belongs to the "admin" group and therefore granted
     "admin" privileges, a wrench icon will be displayed next to your login
     name. This page allows the admin to view group memberships for all
     users, as well as, edit their profile data.

User profile

:    Shows a page showing group memberships.

Logout

:    Pressing this link ends the login session and will require a subsequent login.

Help

:    Opens a page to the online help system.


#### Now running

The "Now running" section appears at the top of the Run and Jobs pages
and provides a view into the execution queue.
Any currently executing ad hoc command or Job will be listed
and include information like the name of the job, when it started,
who ran it, and a link to the execution output.

![Now running](../figures/fig0215.png)

Jobs that have been run before also have a progress bar approximating 
duration.

### Shell Tools 

Rundeck includes a number of shell tools to dispatch commands, load
and run Job definitions and interact with the dispatcher queue. These
command tools are an alternative to functions accessible in the
graphical console.

[dispatch]
  ~ Execute ad hoc commands and scripts
[rd-queue]
  ~ Query the dispatcher for currently running Jobs and possibly kill them  
[rd-jobs]
  ~ List defined jobs as well as load them from text file definitions
[run]
  ~ Invoke the execution of a stored Job
[rd-project]
  ~ Setup a new Rundeck project
[rd-setup]
  ~ (Re-)configure an instance of Rundeck   
  
Consult the online manual pages for options and usage information.

[dispatch]: dispatch.html
[rd-queue]: rd-queue.html
[rd-jobs]: rd-jobs.html
[run]: run.html
[rd-project]: rd-project.html
[rd-setup]: rd-setup.html


## Project Setup 

A Rundeck *Project* provides a space to manage related management
activities. 

A Project can be set up either from the graphical console or using the
`rd-project` shell tool.

After logging into the graphical console, you will notice a Project
menu in the top navigation bar. If no projects exist, you will be prompted to 
create a new project.

![Create project prompt](../figures/fig0203-a.png)

To start with, the only field you need to enter is the Project Name. You can
change the other values later from the [GUI Admin Page](../administration/configuration.html#gui-admin-page).

After entering your project name, Rundeck initializes it and returns
you to the "Run" page.

Projects can be created at any time by going back to the Project menu 
and selecting the "Create a new project..." item.

![Create project menu](../figures/fig0203.png)

The `rd-project` shell tool can also be used to create a
project.

On the Rundeck server, execute the `rd-project` command and
specify a project name, here we use "examples":

    rd-project -a create -p examples

After running this command, you can login into the graphical console
and see the new project in the project menu.

You can also add configuration properties when you create the project, for example:

    rd-project -a create -p examples --project.ssh-keypath=/private/ssh.key

The project setup process generates Project configuration in the server, and
a bootstrap resource model.

![Run page after new project](../figures/fig0204.png)

One node will be listed, the Rundeck server host. The server host is 
distinguished with the word "server" in red text.

### Resource model

The Resource Model is the set of available Nodes that Rundeck can dispatch commands to, and their associated metadata. Each Rundeck Project has its own Resource Model.

The initial resource model will contain
information just about the Rundeck server host and is useful just for
running local commands on the Rundeck server. 
You can browse the project resource model by going to the "Run" page.

In the shell, you can list the Node resources in a resource
model using the shell tool, `dispatch`. 
Specify project name using the `-p project` option.

Here the `dispatch` command lists the registered server for
the "examples" project after the project setup. The `-v` gives
a verbose listing that includes more detail:

    $ dispatch -p examples -v	
     strongbad:
        hostname: strongbad
        osArch: x86_64
        osFamily: unix
        osName: Mac OS X
        osVersion: 10.6.2
        tags: ''

Node resources have standard properties, such as "hostname" but these
can be extended via attributes. One of the more useful properties
is the "tags" property. A *tag* is a text label that you give to the
Node, perhaps denoting a classification, a role the node plays in the
environment, or group membership. Multiple tags can be defined for
a given node. 

The output above shows the "strongbad" node currently has an empty
tags property: `tags: ''`. 

It is important to start thinking about node tagging for the nodes you manage
because you will use them later when specifying filtering
options to drive distributed command dispatch.

Each Project has a configuration file called [project.properties](../administration/configuration.html#project.properties),
located at this path:
`$RDECK_BASE/projects/`_project_`/etc/project.properties`.

This configuration file contains two basic properties for accessing and
storing resource model data:

* `project.resources.file`: A local file path to read a resource model document
* `project.resources.url`: URL to an external resource model source (optional)

In addition, multiple pluggable "Resource Model Sources" can be configured for a project
to retrieve additional Resource Model content from other sources. See [Resource Model Sources](plugins.html#resource-model-sources).

You can configure Rundeck to retrieve and store resource model data
from any source, so long as it can produce one of the Rundeck resource model
document formats. (See 
[Resource Model Document formats](rundeck-basics.html#resource-model-document-formats).) Set the 
`project.resource.url` to the URL resource model source of your choice.

Here's the XML document stored for the "examples" project that corresponds
to the output printed by the `dispatch -v` shown earlier:

    <project>
        <node name="strongbad" type="Node" 
          description="the Rundeck server host" tags="" 
          hostname="strongbad" 
          osArch="x86_64" osFamily="unix" osName="Mac OS X" osVersion="10.6.2"
          username="alexh" 
          editUrl="" remoteUrl=""/>
    </project>

You'll notice the root node is called `project` and there is a single
node descriptor for "strongbad". The `node` tag has a number of
required and optional attributes. Additional node descriptors can be
added by defining new `node` elements inside the `project` tag. 

The strongbad host does not have any tags defined for it. One or
more tags can be defined. Use comma for the delimiter (e.g, `tags="tag1,tag2"`).

Here's an example of a node called "homestar" with just the required
attributes: 

        <node name="homestar" type="Node" 
          hostname="192.168.1.02" 
          username="alexh" />

The `hostname` and `username` values are used for the SSH connection
while the `name` and `type` are used to define Node identity in the
resource model. It is possible to overload the hostname value to include
port information (eg, hostname="somehost:2022"). 
This is useful if your run SSH on a different port. 
	  
Chances are you maintain information about your hosts within
another tool, perhaps Chef, Puppet, Nagios, Amazon EC2, RightScale or
even an in-house database. One of these tools might be
considered the authority of knowledge about the nodes
deployed in your network. Therefore, it is best to create an interface
to the authoritative tool and expose it as Rundeck URL resource model source. This
can be done as a simple CGI script that does a transformation from
the tool's format to the one Rundeck understands.

Of course, a rudimentary alternative is to maintain this information
as an XML document, storing it in a source repository that is
periodically exported to Rundeck. This method could be practical if
your host infrastructure infrequently changes.

Check the Rundeck web site for URL resource model sources. If you are
interested in creating your own, see the
[Resource model source](../administration/node-resource-sources.html#resource-model-source) section in the
[Integration with External Data Providers](#integration-with-external-data-providers) chapter.

### Resource Model Document formats

Rundeck currently has two resource model document formats built in: 

* XML: [resource-v13(5) XML](../manpages/man5/resource-v13.html).  Format name: `resourcexml`.
* Yaml: [resource-v13(5) YAML](../manpages/man5/resource-yaml-v13.html). Format name: `resourceyaml`.

You can enable more formats using [Resource Format Plugins](plugins.html#resource-format-plugins).

## Pluggable Resource Model Sources

Each project can have multiple sources for Resource model information, and
you can use or write plugins to enable new sources for entries in the Resource model.

You can configure the sources via the GUI from the Admin page, see
[GUI Admin Page](../administration/configuration.html#gui-admin-page), or by modifying the project configuration file,
see [Resource Model Sources](plugins.html#resource-model-sources).

## Command Execution

Rundeck supports two modes of execution: *ad-hoc commands* and *Job*.

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

:    Concurrency
     is controlled by setting the "threadcount". Execution can continue even if
     some node fails if the "keepgoing" option is set to true.

Node Filters

:    Filtering options specify include and exclude filters to
     determine which nodes from the project resource model to distribute
     commands to.

Filter Keywords

:    Keywords are used within they include and exclude patterns. The
     "tags" keyword additionally can use a boolean operator to combine
     logical ORs and ANDs.

Filter combinations

:    All keywords can be combined by specifying the include and
     exclude options multiple times on the command line.
  
One can experiment querying the resource model in the graphical
console or with the `dispatch` tool.

#### Filtering nodes graphically  

A project's Node resources are displayed in the Run page. Use the
project menu in the navigation bar to change to the desired project.
After choosing a project, the server node will be filtered by default.

Nodes can be filtered using include and exclude patterns by using
the Filter form. The form can be opened by pressing the "Filter" button.
Press the triangular disclosure icon to display the form.

![Resource filter link](../figures/fig0205.png)

When the form opens, you will see it divided into an Include section
where simple include expressions can be set, as well as, an "Extended
Filters..." link where exclude expressions can be made. 

![Resource filter form](../figures/fig0206.png)

After filling out the filter form, press "Filter" to generate a new
listing. Pressing "Clear" resets the form.

The Include and Exclude filters allow for filtering nodes based on the
following keywords: Name, Tags, Hostname, OS Name, OS Family, OS
Architecture, OS Version and Type.

Regular expressions can be used for any of the keywords. The ``.*`` pattern
will match any text.

If more than 20 nodes match the filter, the UI will page the results.
  
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

Consult the [rd-options(1)](../manpages/man1/rd-options.html) manual page for the complete reference on
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
    Succeeded queueing Workflow execution: Workflow:(threadcount:1){ [command( exec: uptime)] }
    Queued job ID: 7 <http://strongbad:4440/execution/follow/7>

The ``uptime`` command is queued and executed. The output can be followed by
going to the URL returned in the output (eg, http://strongbad:4440/execution/follow/7). 

Sometimes it is desirable to execute the command
directly, and not queue it [^noqueue]. Use the ``--noqueue`` option to execute
and follow the output from the console.

    $ dispatch -I os-family=unix  --noqueue -- uptime
    [ctier@centos54 dispatch][INFO]  10:34:54 up 46 min,  2 users,  load average: 0.00, 0.00, 0.00
    [alexh@strongbad dispatch][INFO] 10:34  up 2 days, 18:51, 2 users, load averages: 0.55 0.80 0.75
    [examples@ubuntu dispatch][INFO]  10:35:01 up 2 days, 18:40,  2 users,  load average: 0.00, 0.01, 0.00

[^noqueue]: The "--noqueue" flag is useful for testing and debugging execution
but undermines visibility since execution is not managed through the central execution
queue.

Notice, the `dispatch` command prepends the message output
with a header that helps understand from where the output originates. The header
format includes the login and node where the `dispatch` execution
occurred.

Execute the Unix `whomi` command to see what user ID is
used by that Node to run dispatched commands:

    $ dispatch -I os-family=unix --noqueue -- whoami
    [ctier@centos54 dispatch][INFO] ctier
    [alexh@strongbad dispatch][INFO] alexh
    [examples@ubuntu dispatch][INFO] examples

You can see that the resource model defines each Node to use a
different login to execute `dispatch` commands.  That
feature can be handy when Nodes serve different roles and therefore,
use different logins to manage processes. See the
`username` attribute in [resource-v13(5) XML](../manpages/man5/resource-v13.html) or [resource-v13(5) YAML](../manpages/man5/resource-yaml-v13.html) manual page.

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

The Rundeck graphical console also provides the ability to execute
ad-hoc commands to a set of filtered Node resources.
The command prompt can accept any ad-hoc command string you might run
via an SSH command or via the `dispatch` shell tool.

But before running any commands, you need to select the project
containing the Nodes you wish to dispatch. Use the project
menu to select the desired project name. After the project has been
selected you will see a long horizontal textfield labeled
"Command". This is the Rundeck ad hoc command prompt.

![Ad hoc command prompt](../figures/fig0207.png)

To use the command prompt, type the desired ad-hoc command string into
the textfield and press the "Run" button. The command will be
dispatched to all the Node resources currently listed below the
command prompt tool bar. The command prompt also becomes disabled until
the execution completes. Output from the command execution will be shown
below (see [output](rundeck-basics.html#following-execution-output)).

![Ad hoc execution output](../figures/fig0208.png)

You will also notice the ad hoc execution listed in the "Now running" 
part of the page, located above the command prompt.
All running executions are listed there. Each running execution
is listed, showing the start time, the user running it, and a link
to follow execution output on a separate page.

![Now running ad hoc command](../figures/fig0207-b.png)

At the bottom of the page, you will see a "History" section containing
all executions in the selected project for the last 24 hours. After the execution
completes, a new event will be added to the history. A yellow highlight
indicates when the command leaves the Now running section and enters
the history table.

![Run history](../figures/fig0207-c.png)

History is organized in summary form using a table layout. The "Summary" column
shows the command or script executed. The "Node Failure Count" contains
the number of nodes where an error in execution occurred. If no errors occurred,
"ok" will be displayed. The "User" and "Time" columns show the user that executed
the command and when.

##### Following execution output

Ad hoc command execution output is displayed below the command prompt.

This page section provides several views to read the output using different formats.

Tail Output

:   Displays output messages from the command execution as if you were
    running the Unix `tail -f` command on the output log file. 
    By default, only the last 20 lines of output is displayed but this
    can be expanded or reduced by pressing the "-" or "+" buttons. You
    can also type in an exact number into the textfield.
    ![Ad hoc execution output](../figures/fig0208.png)

Annotated

:   The annotated mode displays the output messages in the order they
    are received but labels the each line with the Node from which the
    message originated. Through its additional controls each Node
    context can be expanded to show the output it produced, or
    completely collapsed to hide the textual detail.    
    ![Annotated output](../figures/fig0209.png)

Compact

:   Output messages are sorted into Node specific sections and are not
    interlaced. By default, the messages are collapsed but can be
    revealed by pressing the disclosure icon to the right. 
    ![Node output](../figures/fig0210.png)

###### Separate execution follow page

Sometimes it is useful to have a page where just the execution output
is displayed separately. One purpose is to share a link to others 
interested in following the output messages. Click the "output >>"
link in the "Now running" section to go to the execution follow page.

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
"-C" option specifies the number of execution threads. Here's an
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

    $ dispatch -I os-family=unix -s /tmp/listening.sh --noqueue
    [alexh@strongbad dispatch][INFO] Connecting to centos54:22
    [alexh@strongbad dispatch][INFO] done.
    [ctier@centos54 dispatch][INFO] not listening on 4440
    error: Remote command failed with exit status 1

The script failed on centos54 and caused dispatch to error out immediately.

Running the command again, but this time with the "-K" keepgoing flag
will cause dispatch to continue and print on which nodes the script
failed:

    $ dispatch  --noqueue -K -I tags=web -s /tmp/listening.sh
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
	
### Queuing commands to Rundeck

By default, commands or scripts executed on the command line by `dispatch` are
queued as temporary jobs in Rundeck. The `dispatch` command
is equivalent to a "Run and Forget" action in the graphical console.

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

Running `dispatch` causes the execution to queue in
Rundeck and controlled as  temporary Job. The `-I centos54` limits
execution to just the "centos54" node:

    $ dispatch -I centos54 -s ~/bin/checkagain.sh 
    Succeeded queueing workflow: Workflow:(threadcount:1){ [command( scriptfile: /Users/alexh/bin/checkagain.sh)] }
    Queued job ID: 5 <http://strongbad:4440/execution/follow/4>

To pass arguments to the script pass them after the "\--" (double
dash):

    $ iters=5 secs=60 port=4440
    $ dispatch -I centos54 -s ~/bin/checkagain.sh -- $iters $secs $ports


### Tracking execution

Queued ad-hoc command and temporary or saved Job executions can be
tracked from the "Run" page in the "[Now Running](rundeck-basics.html#now-running)" area at the top of
the page.

Execution can also be tracked using the [rd-queue](../manpages/man1/rd-queue.html) shell tool.

    $ rd-queue -p project
    Queue: 1 items
    [5] workflow: Workflow:(threadcount:1){[command( scriptfile: /Users/alexh/bin/checkagain.sh)]} <http://strongbad:4440/execution/follow/5>

Each job in the execution queue has an execution ID. The example above
shows one item with the ID, 5.

Running jobs can also be killed via `rd-queue kill`. 
Specify execution ID using the "-e" option:

    $ rd-queue kill -e 5
    rd-queue kill: success. [5] Job status: killed

### Plugins

Rundeck supports a plugin model for the execution service, which allows you to
fully customize the way that a particular Node, Project or your entire Rundeck installation executes commands and scripts remotely (or locally).

By default Rundeck uses an internal plugin to perform execution via SSH for remote nodes, 
and local execution on the Rundeck server itself.

Plugins can be installed by copying them to the `libext` directory of your Rundeck
installation.

Plugins are used to add new "providers" for particular "services".  The services used for  command execution on nodes are the "node-executor" and "file-copier" services.

To use a particular plugin, it must be set as the provider for a service by configuring the `framework.properties` file, the `project.properties` file, or by adding attributes to Nodes in your project's resources definitions.  For more about configuring the providers, see [Using Providers](plugins.html#using-providers).

The internal SSH command execution plugin is described below, and more information about plugins can be found in the [Rundeck Plugins](#rundeck-plugins) chapter.

#### SSH Plugin

Rundeck by default uses SSH to execute commands on remote nodes, SCP to copy scripts to remote nodes, and locally executes commands and scripts for the local (server) node.

The SSH plugin expects each node definition to have the following properties in order to create the SSH connection:

* `hostname`: the hostname of the remote node.  It can be in the format "hostname:port" to indicate that a non-default port should be used. The default port is 22.
* `username`: the username to connect to the remote node.

When a Script is executed on a remote node, it is copied over via SCP first, and then executed.  In addition to the SSH connection properties above, these node attributes
can be configured for SCP:

* `file-copy-destination-dir`: The directory on the remote node to copy the script file to before executing it. The default value is `C:/WINDOWS/TEMP/` on Windows nodes, and `/tmp` for other nodes.
* `osFamily`: specify "windows" for windows nodes.

In addition, for both SSH and SCP, you must either configure a public/private keypair for the remote node or configure the node for SSH Password authentication.

* See [Administration - SSH](../administration/ssh.html) for more information on setting up your SSH server
* See [SSH Provider](plugins.html#ssh-provider) for more information on the configuration of Nodes for SSH

#### Included Plugins

Two plugin files are included with the default Rundeck installation for your use in testing or development. (See [Pre-Installed Plugins](plugins.html#pre-installed-plugins))

* [Stub plugin](plugins.html#stub-plugin): simply prints the command or script instead of running it.
* [Script plugin](plugins.html#script-plugin): executes an external script file to perform the command, useful for developing your own plugin with the [Script Plugin Development](../developer/plugin-development.html#script-plugin-development) model.

## History

History for queued ad-hoc commands, as well as, temporary and
saved Job executions  is stored by the Rundeck server. History data
can be filtered and viewed inside the "History" page.

![History page](../figures/fig0211.png)

### Filtering event history

By default, the History page will list history for the last day's
executions. The page contains a filter control that can be used to
expand or limit the executions.

The filter form contains a number of fields to limit search:

* Within: Time range. Choices include 1 day, 1 week, 1 month or other
  (given a start after/before to ended after/before).
* Name: Job title name.
* Project: Project name. This may be set if the project menu was used.
* User: User initiating action.
* Summary: Message text.
* Result: Success or failure status.

![History filter form](../figures/fig0212.png)

After filling the form pressing the "Filter" button, the page will
display events matching the search.

Filters can be saved to a menu that makes repeating searches more
convenient. Click the "save this filter..." link to save the filter
configuration.

### Event view

History for each execution contains the command(s) executed,
dispatcher options, success status and a link to a file containing all
the output messages.

![Event view](../figures/fig0213.png)

If any errors occurred, the "Node Failure Count" column will show
the number of nodes in red text. A bar chart indicates the percent
failed.

![Event view](../figures/fig0216.png)

### RSS link

An RSS icon provides a link to an RSS view of the events that match
the current filtering criteria.

![RSS link](../figures/fig0214.png)

## Tips and Tricks 

### Saving filters

Each of the filter controls provides the means to save the current
filter configuration. Press the "save this filter..." link to give it
a name. Each saved filter is added to a menu you can access the next
time you want that filter configuration.

### Auto-Completion

If you use the Bash shell, Rundeck comes with a nice auto-completion
script you can enable. Add this to your `.bashrc` file:

    source $RDECK_BASE/etc/bash_completion.bash
  
Press the Tab key when you're writing a dispatch command, and it should
return a set of suggestions for you to pick from:

    $ dispatch <tab><tab>

## Summary

At this point, you can do basic Rundeck operations - setup a project,
define and query the project resource model, execute ad-hoc
commands, run and save Jobs and view history.

Next, we'll cover one of Rundeck's core features: Jobs.
