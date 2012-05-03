% JOB-V20(5) Rundeck User Manuals | Version 2.0
% Alex Honor
% November 20, 2010

# NAME

job-v20 - The 'job' XML file declares job entries for Rundeck.

This is a demonstration document using all possible elements in the
current Rundeck "jobs" XML.

## Loading and unloading

This file can be batch loaded via *rd-jobs* load command:

    rd-jobs load --file /path/to/jobs.xml

Rundeck job definitions can be dumped and saved to a file via
rd-jobs list command:

    rd-jobs list --file /tmp/jobs.xml

# joblist 

The root (aka "top-level") element of the jobs XML file. 

*Nested elements*

[job](#job)*

:    declares a single job

*Example*

    <joblist>
      <job>
       ...
      </job>
      <job>
       ...
      </job>
    </joblist>


## job

The job element is a sub-element of [joblist](#joblist) and defines a job
executable in Rundeck.

The following elements are used to describe the job. Only one of each
element is allowed.

*Nested elements*

[uuid](#uuid)

:    unique UUID to identify the job

[name](#name-1)

:    the job name

[description](#description)

:    the job description

[group](#group)

:    group name

[multipleExecutions](#multipleexecutions)

:    If the job can be executed multiple times simultaneously

[context](#context)

:    command context

[dispatch](#dispatch)

:    dispatch options

[sequence](#sequence)

:    workflow sequence

[notification](#notification)

:    notifications of execution success/failure, via email or webhook

[nodefilters](#nodefilters)

:    node filtering expressions

[loglevel](#loglevel)

:    the logging level

*Job command modes*
     
Jobs execute a sequence of commands. Commands come in several styles:

* System command
* A script
* A script file or URL
* Another defined job

*Examples*

Execute the Unix 'who' command

    <joblist>
      <job>
        <name>who's logged in?</name>
        <description>Runs the unix who command</description>
        <group>sysadm/users</group>
        <context>
          <project>default</project>
        </context>
        <sequence>
          <command>
            <!-- the Unix 'who' command -->
            <exec>who</exec>
          </command>
         </sequence>
        <nodefilters excludeprecedence="true">
          <include>
            <os-family>unix</os-family>
          </include>
        </nodefilters>
        <dispatch>
          <threadcount>1</threadcount>
          <keepgoing>true</keepgoing>
        </dispatch>
      </job>
    </joblist>

Execute a Bash script

    <joblist>
      <job>
        <name>a simple script</name>
        <description>Runs a trivial bash script</description>
        <group>sysadm/users</group>
        <context>
          <project>default</project>
        </context>
        <sequence>
          <command>
            <script><![CDATA[#!/bin/bash
    echo this is an example job running on $(hostname)
    echo whatever
    exit 0 ]]></script>
          </command>
         </sequence>
        <dispatch>
          <threadcount>1</threadcount>
          <keepgoing>true</keepgoing>
        </dispatch>
      </job>
    </joblist>

Execute a sequence of other commands, scripts and jobs:

    <joblist>
      <job>
        <name>test coreutils</name>
        <description/>
        <context>
          <project>default</project>
        </context>
        <sequence>         
         <!-- the Unix 'who' command -->
         <command>
            <exec>who</exec>
         </command>
         <!-- a Job named test/other tests -->
         <command>
            <jobref group="test" name="other tests"/>
         </command>
        </sequence>
        <dispatch>
          <threadcount>1</threadcount>
          <keepgoing>false</keepgoing>
        </dispatch>
      </job>
    </joblist>

## uuid

The UUID is a sub-element of [job](#job). This string can be set manually (if
you are writing the job definition from scratch), or will be assigned at job
creation time by the Rundeck server using a random UUID.  This string should be 
as unique as possible if you set it manually.

This identifier is used to uniquely identify jobs when ported between Rundeck
instances.

## name 

The job name is a sub-element of [job](#job). The combination of  'name'
and  [group](#group) and [project](#project) must be unique if the [uuid](#uuid) identifier is not
included.
     
## description 

The job description is a sub-element of [job](#job) and allows a short
description of the job.

## group 

The group is a sub-element of [job](#job) and defines the  job's group
identifier. This is a "/" (slash) separated string that mimics a
directory structure. 

*Example*

    <job>
        <name>who</name>
        <description>who is logged in?</description>
        <group>/sysadm/users</group>
    </job>

## multipleExecutions

Boolean value: 'true/false'.  If 'true', then the job can be run multiple times at once.  Otherwise, the Job can only have a single execution at a time.

    <job>
        <name>who</name>
        <description>who is logged in?</description>
        <group>/sysadm/users</group>
        <multipleExecutions>true</multipleExecutions>
    </job>

## schedule
     
<code>schedule</code> is a sub-element of [job](#job) and specifies
periodic job execution using the stated schedule.  The schedule can be
specified using embedded elements as shown below, or using a single
[crontab](#crontab) attribute to set a full crontab expression.

*Nested elements*

[time](#time)

:    the hour and minute and seconds

[weekday](#weekday)

:    day(s) of week

[month](#month)

:    month(s)

[year](#year)

:    year

*Attributes*

[crontab](#crontab)

:   a full crontab expression


*Example*

Run the job every morning at 6AM, 7AM and 8AM.

    <schedule>
	 <time hour="06,07,08" minute="00"/>
	 <weekday day="*"/>
	 <month month="*"/>
     </schedule>

Run the job every morning at 6:00:02AM, 7:00:02AM and 8:00:02AM only
in the year 2010:


    <schedule>
	 <time hour="06,07,08" minute="00" seconds="02"/>
	 <weekday day="*"/>
	 <month month="*"/>
	 <year year="2010"/>
    </schedule>

Run the job every morning at 6:00:02AM, 7:00:02AM and 8:00:02AM only
in the year 2010, using a single crontab attribute to express it:

    <schedule crontab="02 00 06,07,08 ? * * 2010"/>

For more information, see
http://www.quartz-scheduler.org/docs/tutorials/crontrigger.html
or http://www.stonebranch.com 


### crontab ###

Attribute of the [schedule](#schedule), sets the schedule with a full
crontab string. For more information, see
http://www.quartz-scheduler.org/docs/tutorials/crontrigger.html. 

If specified, then the embedded schedule elements are not used.

### time

The [schedule](#schedule) time to run the job

*Attributes*

hour

:    values: 00-23

minute

:    values: 00-59

 
### weekday
     
The [schedule](#schedule) weekday to run the job

*Attributes*

day

:   values: `*` - all ; `1-5` days "sun-sat" ; `1,2,3-5` - days "sun,mon,tue-thu", etc


### month 
     
The [schedule](#schedule) month to run the job

*Attributes*

month

:    values: * - all 1-10 - month jan-oct 1,2,3-5 - months jan,feb,mar-may, etc.

day

:    day of the month: * - all; 1-31 specific days

## context 
     
The [job](#job) context.

*Nested elements*

[project](#project)

:    the project name (required)

[options](#options)

:    job options. specifies one or more option elements


### project 

The [context](#context) project name.

### options
     
The [context](#context)  options that correspond to the called [command](#command).

*Nested elements*

option](#option)

:    an option element

*Example*

    <options>
        <option name="detail" value="true"/>
    </options>

#### option

Defines one option within the [options](#options).

*Attributes*

name               

:    the option name 

value

:    the default value

values

:    comma separated list of values

valuesUrl

:    URL to a list of JSON values

enforcedvalues

:    Boolean specifying that must pick from one of values

regex

:    Regex pattern of acceptable value

description

:    Description of the option

required

:    Boolean specifying that the option is required

multivalued

:    "true/false" - whether the option supports multiple input values

delimiter

:    A string used to conjoin multiple input values.  (Required if `multivalued` is "true")

secure

:   "true/false" - whether the option is a secure input option. Not compatible with "multivalued"

*Example*

Define defaults for the "port" option, requiring regex match. 

    <option name="port" value="80" values="80,8080,8888" regex="\d+"/>

Define defaults for the "port" option, enforcing the values list.

    <option name="port" value="80" values="80,8080,8888" enforcedvalues="true" />

Define defaults for the "ports" option, allowing multiple values separated by ",".

    <option name="port" value="80" values="80,8080,8888" enforcedvalues="true" multivalued="true" delimiter="," />


#### valuesUrl JSON 

The data returned from the valuesUrl can be formatted as a list of values:

    ["x value","y value"]

or as Name-value list:

    [
      {name:"X Label", value:"x value"},
      {name:"Y Label", value:"y value"},
      {name:"A Label", value:"a value"}
    ] 

## dispatch 

     
The [job](#job) dispatch options. See the [Dispatcher options] for
general information.

*Nested elements*

[threadcount](#threadcount)

:    dispatch up to threadcount

[keepgoing](#keepgoing)

:    keep going flag

[rankAttribute](#rankAttribute)

:    Name of the Node attribute to use for ordering the sequence of nodes (default is "nodename")

[rankOrder](#rankOrder)

:    Order direction for node ranking. Either "ascending" or "descending" (default "ascending")

*Example*

    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
      <rankAttribute>nodename</rankAttribute>
      <rankOrder>descending</rankOrder>
    </dispatch>

### threadcount

Defines the number of threads to execute within [dispatch](#dispatch). Must be
a positive integer.

### keepgoing 

Boolean describing if the [dispatch](#dispatch) should continue of an error
occurs (true/false). If true, continue if an error occurs.

### rankAttribute

This is the name of a Node attribute that determines the order in which the Nodes are traversed.  The default value of "nodename" will rank the nodes based on their names.

This can be any attribute of a Node, even attributes that do not exist on some nodes.  For example you can set it to "rank", then any Nodes with a "rank" attribute will be ordered before any other nodes, and they will be used in the order of the rank attribute value.

The values in the rank attribute are compared first numerically if they are valid integers, but otherwise they are compared alphanumerically.  Nodes which do not have the specified rank attribute will be ordered by node name and treated as if they come after all nodes which do have the rank attribute (if in ascending order).

### rankOrder

This determines whether the rank attribute should be used to order the nodes in ascending or descending order.

Possible values: "ascending", or "descending".  The default if not specified is "ascending".

## loglevel 

The [job](#job) logging level. The lower the more profuse the messages.

* DEBUG
* VERBOSE
* INFO
* WARN
* ERR

## nodefilters
     
The [job](#job) nodefilters options. See  [Include/exclude patterns](#includeexclude-patterns) for a
general description.

*Attributes*

excludeprecedence

:    boolean value: true or false

*Nested elements*

[include](#include)

:    include filter

[exclude](#exclude)

:    exclude filter


*Example*

    <nodefilters excludeprecedence="true">
      <include>
        <hostname/>
        <type/>
        <tags>tomcats</tags>
        <os-name/>
        <os-family/>
        <os-arch/>
        <os-version/>
        <name/>
      </include>
    </nodefilters>


### include

See [Include/exclude patterns](#includeexclude-patterns)

### exclude

See [Include/exclude patterns](#includeexclude-patterns)

### Include/exclude patterns 

The [nodefilters](#nodefilters) include and exclude patterns.

*Nested elements*

hostname

:    node hostname

name

:    node resource name

type

:    node type

tags

:    node tags. comma separated

os-name

:    operating system name (eg, Linux, Mac OS X)

os-family

:    operating system family (eg, unix, windows)

os-arch

:    operating system architecture (eg i386,sparc)

os-version

:    operating system version


## sequence 

The [job](#job) workflow sequence.  

*Attributes*

keepgoing

:    true/false. (default false). If true, the workflow sequence will continue even if there is a failure 

strategy

:    node-first/step-first. (default "node-first"). The strategy to use for executing the workflow across nodes.

The strategy attribute determines the way that the workflow is
executed. "node-first" means execute the full workflow on each node
prior to the next.  "step-first" means execute each step across all
nodes prior to the next step.

*Nested elements*

[command](#command)

:    a sequence step


### command

Defines a step for a workflow [sequence](#sequence).

The different types of sequence steps are defined in different ways.

See:

* [Script sequence step](#script-sequence-step)
* [Job sequence step](#job-sequence-step)


 
#### Script sequence step 

Script steps can be defined in three ways within a command element:

* Simple shell command using <code>exec</code> element.
* Embedded script using <code>script</code> element.
* Script file using <code>scriptfile</code> and <code>scriptargs</code> elements.

Example exec step:


    <command>
       <exec>echo this is a shell command</exec>
    </command>

Inline script.  Note that using CDATA section will preserve linebreaks
in the script.  Simply put the script within a <code>script</code>
element:


    <command>
        <script><![CDATA[#!/bin/bash
    echo this is a test
    echo whatever
    exit 2 ]></script>
    </command>


Script File:

    <command >
        <scriptfile>/path/to/a/script</scriptfile>
        <scriptargs>-whatever something</scriptargs>
    </command>      

Script URL:

    <command >
        <scripturl>http://example.com/path/to/a/script</scripturl>
        <scriptargs>-whatever something</scriptargs>
    </command>      


#### Job sequence step

Define a [jobref](#jobref) element within the [command](#command) element

##### jobref 

*Attributes*

name

:    the job name

group

:    the group name


*Nested elements*

Optional "arg" element can be embedded:

[arg](#arg)

:    option arguments to the script or job

Example passing arguments to the job:

    <command >
        <jobref group="My group" name="My Job">
           <arg line="-option value -option2 value2"/>
        </jobref>
    </command>      

## notification 

Defines email and webhook notifications for Job success and failure, with in a
[job](#job) definition.

*Nested elements*

[onsuccess](#onsuccess)

:    define notifications for success result

[onfailure](#onfailure)

:    define notifications for failure/kill result

*Example*

    <notification>
        <onfailure>
            <email recipients="test@example.com,foo@example.com" />
        </onfailure>
        <onsuccess>
            <email recipients="test@example.com" />
            <webhook urls="http://example.com?id=${execution.id}" />
       </onsuccess>
    </notification>      

 
### onsuccess 

Embed an [email](#email) element to send email on success, within
[notification](#notification).

Embed an [webhook](#webhook) element to perform a HTTP POST to some URLs, within
[notification](#notification).

### onfailure 

Embed an [email](#email) element to send email on failure or kill,
within [notification](#notification).

Embed an [webhook](#webhook) element to perform a HTTP POST to some URLs, within
[notification](#notification).

### email 

Define email recipients for Job execution result, within
[onsuccess](#onsuccess) or [onfailure](#onfailure).

*Attributes*

recipients

:    comma-separated list of email addresses

*Example*

            <email recipients="test@example.com,dev@example.com" />

### webhook

Define URLs to submit a HTTP POST to containing the job execution result, within [onsuccess](#onsuccess) or [onfailure](#onfailure).

*Attributes*

urls

:   comma-separated list of URLs


*Example*

        <webhook urls="http://server/callback?id=${execution.id}&status=${execution.status}&trigger=${notification.trigger}"/>

* For more information about the Webhook mechanism used, see the chapter [Integration - Webhooks](manual/jobs.html#webhooks).

# SEE ALSO

`rd-jobs` (1).

The Rundeck source code and all documentation may be downloaded from
<https://github.com/dtolabs/rundeck/>.
