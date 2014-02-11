
## Jobs

Jobs are a convenient method to establish a library of routine
procedures. By their nature, a Job encapsulates a process as a
logically named interface. Jobs can begin as a single step workflow
that calls an inline shell script but evolve into a multi-step
workflow, that calls specialized steps.
A job can also call other jobs as steps in its
workflow. Using this approach one can view each Job as a reusable
building block upon which more complex processes can be built.

The administrator decides Jobs can be used to encapsulate
the restart procedures. Both developers and
administrators can collaborate on the job definitions, their evolution and
maintenance. 


## Job structure

The overall goal is to provide a single restart procedure, for the sake of reusability, it
might be preferred to break each step of the process into separate jobs.

Using this approach the administrator imagines the following jobs:

* start: call the start procedure to start the web service
* stop: call the stop procedure to stop the web service
* status: call the status procedure to stop the web service
* Restart: call the stop, start, and status jobs

Since the restart procedure is the primary focus, it is capitalized
for distinction.

The extra complexity from defining a job for every individual step can
pay off later, if those steps can be recombined with future jobs to
serve later management needs. How far a process is decomposed into
individual jobs is a judgement balancing maintenance requirements
and the desire for job reuse.

### Job grouping

It is helpful to use job groups and  have a convention for naming them. 
A good convention assists others with a navigation scheme that
helps them remember and find the desired procedure. Job groups
also help simplify access control policy.

The administrator chooses to create a top level group named
"web" where the web restart related jobs will be organized.

    web/
    |-- Restart
    |-- start
    |-- status
    `-- stop

When opening the "anvils" project users will see the jobs
grouped below web as shown in the screenshot below.

![Anvils job group](../figures/fig0604.png)

## Scripts
Sets of scripts are already in use to manage the startup and shutdown
procedures. Rather than force the issue as to
which one is correct or superior, the administrator focuses on
creating a skeleton to more easily present how scripts can be
encapsulated by the job workflow. After demonstrating this simple
framework, the administrator can discuss how to incorporate the best
of script implementations from the ops and dev teams into the Job definitions.

For the skeleton, the administrator creates placeholder scripts
that merely echo their intent but define the essential arguments they will need.
The scripts - start, status and stop - represent the logical steps of
the restart process.


File listing: start

~~~~~~~~ {.bash .numberLines}
#!/bin/bash
#/ usage: start ?dir?
set -eu
[[ $# != 1 ]] && {
  grep '^#/ usage:' <"$0" | cut -c4- >&2 ; exit 2;
}
DIR=$1
mkdir -p "$DIR"
echo $$ > "$DIR/pid"
echo "- Web started (pid=$$)"
~~~~~~~~ 

File listing: status

~~~~~~~~ {.bash .numberLines}
#!/bin/bash
#/ usage: status ?dir?
set -eu
[[ $# != 1 ]] && {
  grep '^#/ usage:' <"$0" | cut -c4- >&2 ; exit 2;
}
DIR=$1
[[ ! -f "$DIR/pid" ]] && { echo DOWN; exit 1; }
PID=$(cat "$DIR/pid")
[[ -z "$PID" ]] && { echo "DOWN"; exit 1; } || { echo "- RUNNING (pid=$PID)"; }
~~~~~~~~ 

File listing: stop

~~~~~~~~ {.bash .numberLines}
#!/bin/bash
#/ usage: stop ?dir? ?method?
set -eu
[[ $# != 2 ]] && {
  grep '^#/ usage:' <"$0" | cut -c4- >&2 ; exit 2;
}
DIR=$1
METHOD=$2
if [[ -f "$DIR/pid" ]]
then
  pid=$(cat "$DIR/pid")
  rm -f "$DIR/pid"; #approximates a kill process
  exit_code=$?
  echo "- Web stopped (pid=${pid}) using method: $METHOD"
fi
exit ${exit_code:-0}
~~~~~~~~ 

Because either the normal or force can be specified for the
"method" option, the Jobs will need to pass the user's choice as an
argument to the script.

There is no script for the restart process itself since that will be
defined as a Job workflow.

## Job options

To support specifying parameters to the scripts,
the the three jobs will declare an option named "dir"
to specify the web service install directory. The stop
script will need an additional option, "method" to specify `normal` or `force` choices.

A benefit of job options is the ability to display a
menu of choices to the user running the job.  Once chosen, the value
selected by the menu is then passed to the script. 
Options can also have default values or lists of choices to help
the user choose between routine inputs.

### Allowed option values

An option can be defined to only allow values from a specified
list. This places safe guards on how a Job can be run by limiting
choices to those the scripts can safely handle.

The administrator takes advantage of this by limiting the "method" option
values to just  "normal" or "force" choices.

The screenshot below contains the Option edit form for the "method" option.
The form includes elements to define description and default
value, as well as, Allowed Values and Restrictions.

![Option editor for method](../figures/fig0605.png)

Allowed values can be specified as a comma separated list as seen above but
can also be requested from an external source using a "remote URL".

Option choices can be controlled using the "Enforced from values"
restriction. When set "true", the Rundeck UI will only present a
popup menu. If set "false", a text field will also be presented. Use
the "Match Regular Expression" form to validate the input option.

Here's a screenshot of how Rundeck will display the menu choices:

![Option menu for method](../figures/fig0606.png)


### Script access to option data

Option values can be passed to scripts as an argument or referenced
inside the script using a named token. For example, the values for the
"method" an "dir" options can be accessed in one of several ways:

Value referenced as an environment variable. Each option name is upcased
and prefixed with "RD_OPTION_":

* Bash: `$RD_OPTION_METHOD`, `$RD_OPTION_DIR`

Value passed in the argument vector to the executed script or command
via the `scriptargs` tag.

* Commandline Arguments: `${option.method}`, `${option.dir}`

Value represented as a named token inside the script and replaced
before execution:  

* Script Content: `@option.method@`, `@option.dir@`

      
## Job definition

With an understanding of the scripts and the option needed to
control the restart operation, the final step is to compose the Job
definitions. 

While each job can be defined graphically in Rundeck, each can
succinctly be defined using an XML file conforming to the
[job-xml] document format. This 
document contains a set of tags corresponding to the choices seen in
the Rundeck GUI form.

Below are the XML definitions for the jobs. One or more jobs can be
defined inside a single XML file but your convention will dictate how to
organize the definitions. The files can be named any way desired and
do not have to correspond to the Job name or its group.

File listing: stop.xml

~~~~~~~~ {.xml .numberLines}
<joblist>	
    <job> 
       <name>stop</name>  
       <description>the web stop procedure</description>  
       <loglevel>INFO</loglevel>  
       <group>web</group>  
       <context> 
         <options> 
           <option name="method" enforcedvalues="true"
                   required="true" 
               values="normal,force"/> 
           <option name="dir" enforcedvalues="false" required="true" 
             default="$HOME/anvils" /> 
           </options> 
       </context>  
       <sequence threadcount="1" keepgoing="false" strategy="node-first"> 
         <command> 
           <script><![CDATA[#!/bin/bash
#/ usage: stop ?dir? ?method?
set -eu
[[ $# != 2 ]] && {
  grep '^#/ usage:' <"$0" | cut -c4- >&2 ; exit 2;
}
DIR=$1
METHOD=$2
if [[ -f "$DIR/pid" ]]
then
        pid=$(cat "$DIR/pid")
        rm -f "$DIR/pid"; #approximates a kill process
        exit_code=$?
        echo "- Web stopped (pid=${pid}) using method: $METHOD"
fi
exit ${exit_code:-0}]]></script>  
            <scriptargs>${option.method}</scriptargs> 
         </command> 
       </sequence>  
       <nodefilters excludeprecedence="true"> 
         <include> 
           <tags>www</tags> 
         </include> 
       </nodefilters>  
       <dispatch> 
         <threadcount>1</threadcount>  
         <keepgoing>false</keepgoing> 
       </dispatch> 
     </job>
</joblist>
~~~~~~~~~~~~~~~~~

Defines Job, /web/stop, and executes the shell script to
Nodes tagged "web". Using the `scriptargs` tag, the shell
script is passed a single argument, `${option.method}`,
containing the value chosen in the Job run form.

File listing: start.xml

~~~~~~~~ {.xml .numberLines}
<joblist>	
   <job> 
     <name>start</name>  
     <description>the web start procedure</description>  
     <loglevel>INFO</loglevel>  
     <group>web</group>  
    <context/> 
    <context> 
       <options> 
         <option name="dir" enforcedvalues="false" required="true" 
                 default="$HOME/anvils" /> 
       </options> 
    </context> 
    <sequence threadcount="1" keepgoing="false" strategy="node-first"> 
     <command> 
      <script><![CDATA[#!/bin/bash
#/ usage: start ?dir?
set -eu
[[ $# != 1 ]] && {
  grep '^#/ usage:' <"$0" | cut -c4- >&2 ; exit 2;
}
DIR=$1
mkdir -p "$DIR"
echo $$ > "$DIR/pid"
echo "- Web started (pid=$$)"]]></script>
     </command> 
  </sequence>  
    <nodefilters excludeprecedence="true"> 
      <include> 
        <tags>www</tags> 
      </include> 
   </nodefilters>  
   <dispatch> 
     <threadcount>1</threadcount>  
     <keepgoing>false</keepgoing> 
   </dispatch> 
  </job>
</joblist>
~~~~~~~~~~~

Defines Job, /web/start, that also executes a shell script to
Nodes tagged "web".

File listing: status.xml

~~~~~~~~ {.xml .numberLines}
<joblist> 
   <job> 
     <name>status</name>  
     <description>the web status procedure</description>  
     <loglevel>INFO</loglevel>  
     <group>web</group>  
     <context> 
       <options> 
         <option name="dir" enforcedvalues="false" required="true" 
                 default="$HOME/anvils" /> 
       </options> 
     </context>      
     <sequence threadcount="1" keepgoing="false" strategy="node-first"> 
     <command> 
      <script><![CDATA[#!/bin/bash
#/ usage: status ?dir?
set -eu
[[ $# != 1 ]] && {
  grep '^#/ usage:' <"$0" | cut -c4- >&2 ; exit 2;
}
DIR=$1
[[ ! -f "$DIR/pid" ]] && { echo DOWN; exit 1; }
PID=$(cat "$DIR/pid")
[[ -z "$PID" ]] && { echo "DOWN"; exit 1; } || { echo "- RUNNING (pid=$PID)"; }
]]></script>
     </command> 
  </sequence>  
    <nodefilters excludeprecedence="true"> 
      <include> 
        <tags>www</tags> 
      </include> 
   </nodefilters>  
   <dispatch> 
     <threadcount>1</threadcount>  
     <keepgoing>false</keepgoing> 
   </dispatch> 
  </job>
</joblist>
~~~~~~~~~~~

Defines Job, /web/status, that also executes a shell script to
Nodes tagged "web".

> Note, these examples demonstrate Jobs with inline scripts. This is for the purpose of providing a simple and transparent example. You may rightly consider other approaches such as external scriptfiles or custom steps to further encapsulate the code from the job definition.

### Restart Job composition

The final job definition declares the "Restart" job which merely
wraps calls to the stop, start, status jobs already defined.
This is done by declaring a sequence of Job calls,
using the `jobref` xml tag. 
Restart also must pass the "dir" and "method" options so it
declares those too and uses the `arg` xml tag to pass them.

File listing: restart.xml

~~~~~~~~ {.xml .numberLines}
<joblist>	
   <job> 
     <name>Restart</name>  
     <description>restart the web server</description>  
     <loglevel>INFO</loglevel>  
     <group>web</group>  
     <context> 
       <options> 
         <option name="method" enforcedvalues="true" required="false" 
                 values="normal,force" /> 
         <option name="dir" enforcedvalues="false" required="true" 
                 default="$HOME/anvils" /> 
       </options> 
     </context>  
     <sequence threadcount="1" keepgoing="false" strategy="node-first"> 
      <command> 
        <jobref name="stop" group="web">
          <arg line="-dir ${option.dir} -method ${option.method}"/> 
        </jobref> 
      </command>  
      <command> 
        <jobref name="start" group="web">
          <arg line="-dir ${option.dir}"/> 
        </jobref> 
      </command> 
      <command> 
        <jobref name="status" group="web">
          <arg line="-dir ${option.dir}"/> 
        </jobref> 
      </command>       
    </sequence>
   </job>	
</joblist>
~~~~~~~~~~~



Note that we don't define a `<nodefilters>` or `<dispatch>` section for Restart, because we
only want this sequence to execute **once**, on the server node.  The Job
references will each be called once, and the "start", "stop" and "staus" Jobs will
each be dispatched to the nodes they define.

Saving the XML definitions files located on the Rundeck server,
one can load them using the [rd-jobs] command.

Run the `rd-jobs load` command for each job definition file:

~~~~~~~~ {.bash}
rd-jobs load -p anvils -f start.xml   
rd-jobs load -p anvils -f stop.xml    
rd-jobs load -p anvils -f status.xml    
rd-jobs load -p anvils -f restart.xml 
~~~~~~~~

The `rd-jobs list` command queries Rundeck and prints out the list of
defined jobs:

~~~~~~~~ {.bash}
rd-jobs list -p anvils
~~~~~~~~

~~~~~~~~~
Found 3 jobs:
- Restart - 'the web restart procedure'
- start - 'the web start procedure'
- status - 'the web status procedure'
- stop - 'the web stop procedure'
~~~~~~~~~

Of course, the jobs can be viewed inside the Rundeck graphical console by going to
the Jobs page. Clicking the "Restart" job name and clicking the "Definition" tab reveals job detail.

![Anvils Restart job](../figures/fig0607.png)

You will see the composition of the "Restart" job as a workflow
calling the jobs: stop, start, status. The "Restart" job passes the
the `-dir` option to all the jobs and the
`-method` option value to the stop Job.

## Running the job

### Run using the GUI
The Jobs can be run from the Rundeck graphical console by going to the
"Jobs" page. From there, navigate to the "web" job group to
display the three stored Jobs.

Clicking the "Run" button for the Restart job, will display the
options selection page. The menu for the "method" option displays the
two choices: "normal" and "force". No other choices can be made, nor a
textfield for free form entry, because the "method" option was defined
with the restriction "enforced from allowed values".

![Restart run page](../figures/fig0608.png)

### Job run with the CLI

The jobs can also be started from the command line using the
[run] shell tool. The job group and name are specified
using the "-j" parameter. Any options the Job supports are supplied
after the "--" (double dash) parameter. (The "-p" parameter specifies the project,
but it can be left out if there is only one project available.)

Run Restart specifying the method, "normal": 

~~~~~~~~ {.bash}
run -j "web/Restart" -p anvils -- -method normal
~~~~~~~~

Run Restart specifying the method, "force":

~~~~~~~~ {.bash}
run -j "web/Restart" -p anvils -- -method force
~~~~~~~~ 

[job-xml]: ../man5/job-xml.html
[run]: ../man1/run.html
[rd-jobs]: ../man1/rd-jobs.html
