# RunDeck by example

This chapter presents working examples reflecting a variety of
solutions you can create with RunDeck. Helping you apply concepts
and features introduced in earlier chapters is the focus of these examples.
Rather than make the examples abstract, they are set in the context
of Acme Anvils, a fictitious organization that manages an online application. 

## Acme Anvils 

Acme Anvils is a hypothetical start up selling new and used anvils from
their web site.  Two teams inside the company are involved with the
development and support of the anvil sales application. Being a new
company, there isn't much control over access to
the live environment. Either team can make changes to systems which
has led to mistakes and outages. Because the senior management is so
enthusiastic, they push the teams to deliver new features as
frequently as possible. Unfortunately, this has led to another
problem: the Acme Anvil web site is an unstable memory 
hog and requires occasional restarts.  

There are actually two methods to the restart procedure depending
on the problem: "kill" versus "normal". The "kill" restart is required
when the application becomes totally unresponsive. The "normal" restart
occurs when the application needs to free memory.

Depending on the urgency or the staff on hand, either a developer or
an administrator conducts the restart, albeit differently. Because the
developers write the software, they understand the restart
requirements from an application perspective. The administrators on
the other hand, are not always informed of these requirements but are
well versed in restarting the application from a systems
perspective. This has led to a divergence in procedures and has become
the main source of problems that affect their customers.

An administrator, tired of the late night calls to restart the
application, and frustrated by the knowledge gap between operations and
development has decided to take the initiative come up with a better
approach.

## RunDeck set up

The administrator chooses a machine with access to the servers in
the live environment and installs the RunDeck software there.

A project called "anvils" is created to manage the application support
activities. 

The administrator creates the project using the
[rd-project](rd-project.html) shell tool though this could be done with the
RunDeck GUI (see ([project setup](#project-setup)). 
After logging into the RunDeck server, the command is run:

    rd-project -p anvils -a create

This initialized the "anvils" project in RunDeck so it only
contains information about the server node. Adding information about
the nodes deployed in the live environment is the next step 
(see [resource model](#resource-model)).

The environment has five nodes: anv1-anv5. Anvils is a three tier
application and has web, application and database components installed
across the five nodes.

Additionally, the administrator decides to incorporate a recent
convention to use different system logins to execute SSH commands
to control running application components. The web component are run
as the "www" user while the app and database components run as user
"anvils".  

With this information in hand, the administrator prepares the project
resource model using the [resource-v13(5) XML](resource-v13.html) or
[resource-v13(5) YAML](resource-yaml-v13.html) document format. The file listing
below contains the node definitions for the five nodes -- 
anv1, anv2, anv3, anv4, anv5:

File listing: resources.xml

    <?xml version="1.0" encoding="UTF-8"?>

    <project>
      <node name="anv1" type="Node"
         description="an anvils web server" 
         hostname="anv1.acme.com"  username="www" tags="web"/>
      <node name="anv2" type="Node" 
         description="an anvils web server" 
         hostname="anv2.acme.com"  username="www" tags="web"/>
      <node name="anv3" type="Node" 
         description="an avnils app server" 
         hostname="anv3.acme.com"  username="anvils" tags="app"/>
      <node name="anv4" type="Node" 
         description="an anvils app server" 
         hostname="anv4.acme.com"  username="anvils" tags="app"/>
      <node name="anv5" type="Node" 
         description="the anvils database server" 
         hostname="anv5.acme.com"  username="anvils" tags="db"/> 
    </project>

Reviewing the XML content one sees the XML tag set represent
the host information described above. A logical name for each node is defined
with the ``name`` attribute (eg name="anv1"). 
The address used by SSH is set with
``hostname`` (eg hostname="anv1.acme.com") while the login
used to execute SSH commands has been specified with the
``username`` attribute (username="www" vs
username="anvils"). The value for the ``tags`` attribute
reflects the node function  (tags="web" vs tags="app").

The administrator saves the file and places it in a path of his
choice. To make RunDeck aware of it, the administrator modifies the
project configuration file,
$RDECK_BASE/projects/anvils/etc/project.properties, modifying the
``project.resources.file`` setting :

    project.resources.file = /etc/rundeck/projects/anvils/resources.xml
   
With the resources file in place and the project configuration updated, the
administrator has finished with the resource model preparation and can begin
dispatching commands.

List all the nodes in the anvils project by opening the Filter and
typing ``.*`` in the *Name* field and then press "Filter". 
You should see a listing of 6 nodes.

![Anvils resources](figures/fig0601.png)

## Tag classification and command dispatching

With tags that describe application role, commands can be targeted
to specific sub sets of nodes without hard coding any
hostnames. 
The [dispatch](dispatch.html) command's listing feature illustrates
how tag filtering selects particular node sets in the shell:

Use the ``tags`` keyword to list the web nodes:

    dispatch -p anvils -I tags=web
    anv1 anv2
    
List the app nodes:

    dispatch -p anvils -I tags=app
    anv3 anv4

List the db nodes:

    dispatch -p anvils -I tags=db
    anv5

Use the "+" (AND) operator to list the web and app nodes:

    dispatch -p anvils -I tags=web+app
    anv1 anv2 anv3 anv4

Exclude the web and app nodes:

    dispatch -p anvils -X tags=web+app
    anv5

Using a wildcard for node name, list all the nodes:

    dispatch -p anvils -I '.*' 
    anv1 anv2 anv3 anv4 anv5 

Here's an example using filters in the graphical console:

![Anvils filtered list](figures/fig0602.png)

Filtering with tags provides an abstraction over hostnames
and lets the administrator think about scripting process using loose
classifications. New nodes can be added, others decommissioned while
others given new purpose, and the procedures stay unchanged because
they are bound to the filtering criteria. 

This simple classification scheme will allow the developers and
administrators to share a common vocabulary when talking about the kinds
of nodes supporting the Anvils application.

## Jobs

Jobs are a convenient method to establish a library of routine
procedures. By its nature, a saved Job encapsulates a process into a
logically named interface. Jobs can begin as a single item workflow
that calls a small or large shell script but evolve into a multi-step
workflow. One job can also call another job as a step in its
workflow. Using this approach one can view each Job as a reusable
building block upon which more complex automation can be built.

The administrator decides Jobs can be used to encapsulate
procedures to manage the restart process. Both developers and
administrators can collaborate on their definition and evolution and
maintenance. 

Two sets of scripts are already in use to manage the startup and shutdown
procedures. Rather than force the issue as to
which one is correct or superior, the administrator focuses on
creating a skeleton to more easily present how scripts can be
encapsulated by the job workflow. After demonstrating this simple
framework, the administrator can discuss how to incorporate the best
of both script implementations into the Job definitions.

For the skeleton, the administrator creates simple placeholder scripts
that merely echo their intent and the arguments passed to the them.
Two scripts - start.sh and stop.sh - represent the two steps of
the restart process.

Scripts:

File listing: start.sh

    #!/bin/sh
    # Usage: $0 
    echo Web started.

File listing: stop.sh

    #!/bin/sh
    # Usage: $0 [normal|kill]
    echo Web stopped with method: $1.

Because either the normal or kill can be specified for the
"method" option, the Jobs will need to pass the user's choice as an
argument to the script.

There is no script for the restart process itself since that will be
defined as a Job workflow.

### Job structure

With an idea of the restart scripts in mind, the next step is
to define a job to encapsulate the restart procedure. Though the overall goal
is to provide a single restart procedure, for the sake of reusability, it
might be preferred to break each step of the process into separate jobs.

Using this approach the administrator imagines the following jobs:

* start: call the start.sh script to start the web service
* stop: call the stop.sh script to stop the web service
* Restart: calls the jobs: stop, start

Since the restart procedure is the primary focus, it is capitalized
for distinction.

The extra complexity from defining a job for every individual step can
pay off later, if those steps can be recombined with future jobs to
serve later management needs. How far a process is decomposed into
individual jobs is a judgement balancing maintenance requirements
and the desire for job reuse.

### Job grouping

Though not a requirement, it is helpful to use job groups and 
have a convention for naming them. 
A good convention assists others with a navigation scheme that
helps them remember and find the desired procedure.

The administrator chooses to create a top level group named
"/anvils/web/" where the web restart related jobs will be organized.

    anvils/
    `-- web/
        |-- Restart
        |-- start
        `-- stop

After choosing the "anvils" project users will see this
grouping of jobs.

![Anvils job group](figures/fig0604.png)

## Job option

To support specifying the restart method to the scripts,
the the three jobs will declare an option named "method".
Without such a parameter, the administrator would be forced to
duplicate restart Jobs for both the kill and normal stop methods.

Another benefit from defining the job option is the ability to display a
menu of choices to the user running the job.  Once chosen, the value
selected by the menu is then passed to the script.

### Allowed values

An option can be defined to only allow values from a specified
list. This places safe guards on how a Job can be run by limiting
choices to those the scripts can safely handle.

The administrator takes advantage of this by limiting the "method" option
values to just  "normal" or "kill" choices.

The screenshot below contains the Option edit form for the "method" option.
The form includes elements to define description and default
value, as well as, Allowed Values and Restrictions.

![Option editor for method](figures/fig0605.png)

Allowed values can be specified as a comma separated list as seen above but
can also be requested from an external source using a "remote URL".

Option choices can be controlled using the "Enforced from values"
restriction. When set "true", the RunDeck UI will only present a
popup menu. If set "false", a text field will also be presented. Use
the "Match Regular Expression" form to validate the input option.

Here's a screenshot of how RunDeck will display the menu choices:

![Option menu for method](figures/fig0606.png)


### Script access to option data

Option values can be passed to scripts as an argument or referenced
inside the script using a named token. For example, the value for the
"method" option selection can be accessed in one of several ways:

Value referenced as an environment variable:

* Bash: $CT\_OPTION\_METHOD

Value passed in the argument vector to the executed script or command
via the ``scriptargs`` tag:

* Commandline Arguments: ${option.method}

Value represented as a named token inside the script and replaced
before execution:  

* Script Content: @option.method@

      
## Job workflow composition

With an understanding of the scripts and the option needed to
control the restart operation, the final step is to compose the Job
definitions. 

While each job can be defined graphically in RunDeck, each can
succinctly be defined using an XML file conforming to the
[job-v20(5)](job-v20.html) document format. This 
document contains a set of tags corresponding to the choices seen in
the RunDeck GUI form.

Below are the XML definitions for the jobs. One or more jobs can be
defined inside a single XML file but your convention will dictate how to
organize the definitions. The files can be named any way desired and
do not have to correspond to the Job name or its group.

File listing: stop.xml

    <joblist>	
        <job> 
           <name>stop</name>  
           <description>the web stop procedure</description>  
           <loglevel>INFO</loglevel>  
           <group>anvils/web</group>  
           <context> 
               <project>anvils</project>  
                 <options> 
                   <option name="method" enforcedvalues="true"
                           required="true" 
                       values="normal,kill"/> 
                   </options> 
           </context>  
           <sequence threadcount="1" keepgoing="false" strategy="node-first"> 
             <command> 
               <script><![CDATA[#!/bin/sh
    echo Web stopped with method: $1.]]></script>  
                <scriptargs>${option.method}</scriptargs> 
             </command> 
           </sequence>  
           <nodefilters excludeprecedence="true"> 
             <include> 
              <tags>web</tags> 
              </include> 
           </nodefilters>  
           <dispatch> 
             <threadcount>1</threadcount>  
             <keepgoing>false</keepgoing> 
           </dispatch> 
         </job>
    </joblist>


Defines Job, /anvils/web/stop, and executes the shell script to
Nodes tagged "web". Using the ``scriptargs`` tag, the shell
script is passed a single argument, ``${option.method}``,
containing the value chosen in the Job run form.

File listing: start.xml

    <joblist>	
       <job> 
         <name>start</name>  
         <description>the web start procedure</description>  
         <loglevel>INFO</loglevel>  
         <group>anvils/web</group>  
        <context> 
          <project>anvils</project>  
            <options> 
             <option name="method" enforcedvalues="true" required="true" 
              values="normal,kill" /> 
           </options> 
        </context>  
        <sequence threadcount="1" keepgoing="false" strategy="node-first"> 
         <command> 
          <script><![CDATA[#!/bin/sh
     echo Web started. after a $1 shutdown]]></script>  
           <scriptargs>${option.method}</scriptargs> 
         </command> 
      </sequence>  
        <nodefilters excludeprecedence="true"> 
          <include> 
            <tags>web</tags> 
          </include> 
       </nodefilters>  
       <dispatch> 
         <threadcount>1</threadcount>  
         <keepgoing>false</keepgoing> 
       </dispatch> 
      </job>
    </joblist>

Defines Job, /anvils/web/start, that also executes a shell script to
Nodes tagged "web". The shell script is passed a single argument,
``${option.method}``, containing the value chosen in the Job run form.


File listing: restart.xml

    <joblist>	
       <job> 
         <name>Restart</name>  
         <description>restart the web server</description>  
         <loglevel>INFO</loglevel>  
         <group>anvils/web</group>  
         <context> 
           <project>anvils</project>  
             <options> 
               <option name="method" enforcedvalues="true" required="false" 
	          values="normal,kill" /> 
            </options> 
         </context>  
         <sequence threadcount="1" keepgoing="false" strategy="node-first"> 
          <command> 
            <jobref name="stop" group="apps/web"> 
              <arg line="-method ${option.method}"/> 
            </jobref> 
          </command>  
          <command> 
            <jobref name="start" group="apps/web"> 
              <arg line="-method ${option.method}"/> 
            </jobref> 
          </command> 
        </sequence>  
        <nodefilters excludeprecedence="true"> 
         <include> 
           <tags>web</tags> 
         </include> 
        </nodefilters>  
         <dispatch> 
           <threadcount>1</threadcount>  
           <keepgoing>true</keepgoing> 
         </dispatch> 
       </job>	
    </joblist>


Defines Job, /anvils/web/Restart, that executes a sequence of Job calls,
using the ``jobref`` tag.

Saving the XML definitions files located on the RunDeck server,
one can load them using the [rd-jobs](rd-jobs.html) command.

Run the ``rd-jobs load`` command for each job definition file:

    rd-jobs load -f start.xml
    rd-jobs load -f stop.xml
    rd-jobs load -f restart.xml

The ``rd-jobs list`` command queries RunDeck and prints out the list of
defined jobs:

    rd-jobs list
    Found 3 jobs:
	- Restart [9] <http://strongbad:4440/scheduledExecution/show/9>
	- start [10] <http://strongbad:4440/scheduledExecution/show/10>
	- stop [11] <http://strongbad:4440/scheduledExecution/show/11>

Of course, the jobs can be viewed inside the RunDeck graphical console by going to
the Jobs page. Hovering over the "Restart" job name reveals job detail.

![Anvils restart jobs](figures/fig0607.png)

You will see the composition of the "Restart" job as a workflow
calling the jobs: stop and start. The "Restart" job passes the
``-method`` option value to the lower level stop and start Jobs.

## Running the job

The Jobs can be run from the RunDeck graphical console by going to the
"Jobs" page. From there, navigate to the "Anvils/web" job group to
display the three stored Jobs.

Clicking the "Run" button for the Restart job, will display the
options selection page. The menu for the "method" option displays the
two choices: "normal" and "kill". No other choices can be made, nor a
textfield for free form entry, because the "method" option was defined
with the restriction "enforced from allowed values".

![Restart run page](figures/fig0608.png)

The jobs can also be started from the command line using the
[run](run.html) shell tool. The job group and name are specified
using the "-j" parameter. Any options the Job supports are supplied
after the "--" (double dash) parameter.

Run Restart specifying the method, "normal": 

    run -j "anvils/web/Restart" -- -method normal

Run Restart specifying the method, "kill":

    run -j "anvils/web/Restart" -- -method kill


## Job access control

Access to running or modifying Jobs is managed in an access control
policy defined using the aclpolicy document format ([aclpolicy-v10(5)](aclpolicy-v10.html)). 
This file contains a number of policy elements that describe what user
group is allowed to perform which actions. The
[Authorization](#authorization) section of the Administration chapter
covers this in detail.

The administrator wants to use the aclpolicy to define two levels of
access. The first level, has limited privilege and allows for just
running jobs. The second level, is administrative and can modify job
definitions.

Policies can be organized into more than one file to help organize
access by group or pattern of use. The normal RunDeck install will
define two user groups: "admin" and "user" and have a generated a policy
for the "admin" group. 

The Acme administrator decides to create a policy that allows users in
the "user" group to run commands just in the "anvils" and
"anvils/web" Job groups. We can employ the "user" login and group as
it was also included in the normal install.

To create the aclpolicy file for the "user" group:

    cp $RDECK_BASE/etc/admin.aclpolicy $RDECK_BASE/etc/user.aclpolicy

Modify the <command> and <group> elements as shown in the example
below. Notice that just workflow\_read,workflow\_run actions are
allowed.

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~{.xml}
$ cat $RDECK_BASE/etc/user.aclpolicy
<policies>
  <policy description="User group access policy.">
    <context project="*">
      <command group="anvils" job="*" actions="workflow_read,workflow_run"/>
      <command group="anvils/web" job="*" actions="workflow_read,workflow_run"/>
    </context>
    <by>
      <group name="user"/>
    </by>
  </policy>
</policies>
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Restart RunDeck to load the new policy file (see [startup and shutdown](#startup-and-shutdown)).

    rundeckd restart

Once the RunDeck webapp has started, login as the "user" user (the
password is probably "user"). Just the Jobs in the "anvils" group are
displayed in the Jobs page. The "user" user does is not allowed to access
jobs outside of "/anvils group.

Notice the absence of the "New Job" button that would be displayed if
logged in as "admin". Job creation is an action not granted to
"user". Notice also, that the button bar for the listed Jobs does
not include icons for editing or deleting the Job. Only workflow\_read
and workflow\_actions were allowed in the `user.aclpolicy` file.


## Resource model provider examples

See [Chapter 9 - Resource Model Provider](#resource-model-provider).


## Resource editor examples

See [Chapter 9 - Resource Editor](#resource-editor).

## Option model provider examples

See [Chapter 9 - Option Model Provider](#option-model-provider).
