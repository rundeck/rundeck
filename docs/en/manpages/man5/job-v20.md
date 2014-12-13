% JOB-XML
% Alex Honor
% November 20, 2010

# NAME

job-v20 - The 'job' XML file declares job entries for Rundeck.

This is a demonstration document using all possible elements in the
current Rundeck "jobs" XML.

## Loading and unloading

This file can be batch loaded via [rd-jobs] load command:

~~~~~~~~ {.bash}
rd-jobs load -p project --file /path/to/jobs.xml
~~~~~~~~ 

Rundeck job definitions can be dumped and saved to a file via
rd-jobs list command:

~~~~~~~~ {.bash}
rd-jobs list -p project --file /tmp/jobs.xml
~~~~~~~~ 

# joblist 

The root (aka "top-level") element of the jobs XML file. 

*Nested elements*

[job](#job)*

:    declares a single job

*Example*

~~~~~~~~ {.xml}
<joblist>
  <job>
   ...
  </job>
  <job>
   ...
  </job>
</joblist>
~~~~~~~~~~~

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

~~~~~~~~ {.xml .numberLines}
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
~~~~~~~~ 

Execute a Bash script

~~~~~~~~ {.xml .numberLines}
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
~~~~~~~~ 

Execute a sequence of other commands, scripts and jobs:

~~~~~~~~ {.xml .numberLines}
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
~~~~~~~~ 

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

If the description contains more than one line of text, then the first line is used as the "short description" of the job, and rendered exactly as text. The remaining lines are the "extended description", rendered using Markdown format as HTML in the Rundeck GUI. Markdown can also embed HTML directly if you like.  See [Wikipedia - Markdown](http://en.wikipedia.org/wiki/Markdown#Example).  

The HTML is sanitized to remove disallowed tags before rendering to the browser (such as `<script>`, etc.). 
You can disable all extended description HTML rendering
via a configuration flag.
See [GUI Customization](../administration/gui-customization.html).

**Note**: To preserve formatting when defining the extended job description in XML, you should be sure to use a CDATA section. Wrap the contents in `<![CDATA[` and `]]>`.

*Example Extended description*

~~~~~~~~ {.xml }
<job>
    <name>My Job</name>
    <description><![CDATA[Performs a service

This is <b>html</b>
<ul><li>bulleted list</li></ul>

<a href="/">Top</a>

1. this is a markdown numbered list
2. second item

[a link](http://example.com)

]]></description>
</job>
~~~~~~~~ 


## group 

The group is a sub-element of [job](#job) and defines the  job's group
identifier. This is a "/" (slash) separated string that mimics a
directory structure. 

*Example*

~~~~~~~~ {.xml }
<job>
    <name>who</name>
    <description>who is logged in?</description>
    <group>/sysadm/users</group>
</job>
~~~~~~~~ 

## multipleExecutions

Boolean value: 'true/false'.  If 'true', then the job can be run multiple times at once.  Otherwise, the Job can only have a single execution at a time.

~~~~~~~~ {.xml }
<job>
    <name>who</name>
    <description>who is logged in?</description>
    <group>/sysadm/users</group>
    <multipleExecutions>true</multipleExecutions>
</job>
~~~~~~~~ 

## timeout

Timeout string indicating the maximum allowed runtime for a job. After this time expires, the job will be aborted.

The format is:

* `123` a simple number, indicating seconds
* `[number][unit]` where "number" is a valid decimal number, and "unit" is one of:
    * `s` - seconds
    * `m` - minutes
    * `h` - hours
    * `d` - days
* Any sequence of `[number][unit]` pairs.  The total time will be the added value of all the units.  Any other text in the string is ignored, so the pairs can be separated by spaces or other descriptive text.

These are all valid values:

* `1d 6h` - 1 day and 6 hours
* `120m` - 120 minutes
* `${option.timeout}` reference to a job option value


~~~~~~~~ {.xml }
<job>
    <name>who</name>
    <description>who is logged in?</description>
    <group>/sysadm/users</group>
    <timeout>1d 6h</timeout>
</job>
~~~~~~~~ 

## retry

Retry count indicating the maximum number of times to retry the job if it fails or times out. 

Allowed values:

*  An integer number, indicating maximum number of retries
* `${option.retry}` reference to a job option value


~~~~~~~~ {.xml }
<job>
    <name>iffy job</name>
    <description>Job which might need to be retried</description>
    <retry>${option.retry}</retry>
</job>
~~~~~~~~ 

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

~~~~~~~~ {.xml}
<schedule>
  <time hour="06,07,08" minute="00"/>
  <weekday day="*"/>
  <month month="*"/>
</schedule>
~~~~~~~~ 

Run the job every morning at 6:00:02AM, 7:00:02AM and 8:00:02AM only
in the year 2010:

~~~~~~~~ {.xml}
<schedule>
  <time hour="06,07,08" minute="00" seconds="02"/>
  <weekday day="*"/>
  <month month="*"/>
  <year year="2010"/>
</schedule>
~~~~~~~~ 

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

### year

The [schedule](#schedule) year to run the job

*Attributes*

year

:    year: * - all; specific year

## context 
     
The [job](#job) context.

*Nested elements*

[project](#project)

:    the project name (optional)

[options](#options)

:    job options. specifies one or more option elements


### project 

The [context](#context) project name.  Optional. If a project is not specified at import time, this value will be used.

### options
     
The [context](#context) options for user input.

preserveOrder

  :  If set to "true", then the order of the [option](#option) elements will be preserved in
     the Rundeck GUI.  Otherwise the options will be shown in alphabetical order.

*Nested elements*

[option](#option)

:    an option element

*Example*

~~~~~~~~ {.xml}
<options>
    <option name="detail" value="true"/>
</options>
~~~~~~~~ 

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

:    Description of the option, will be rendered as Markdown.

required

:    Boolean specifying that the option is required

multivalued

:    "true/false" - whether the option supports multiple input values

delimiter

:    A string used to conjoin multiple input values.  (Required if `multivalued` is "true")

secure

:   "true/false" - whether the option is a secure input option. Not compatible with "multivalued"

valueExposed

:   "true/false" - whether a secure input option value is exposed to scripts or not. `false` means the option will be used only as a Secure Remote Authentication option.  default: `false`.

*Example*

Define defaults for the "port" option, requiring regex match. 

~~~~~~~~ {.xml}
<option name="port" value="80" values="80,8080,8888" regex="\d+"/>
~~~~~~~~ 

Define defaults for the "port" option, enforcing the values list.

~~~~~~~~ {.xml}
<option name="port" value="80" values="80,8080,8888" enforcedvalues="true" />
~~~~~~~~ 

Define defaults for the "ports" option, allowing multiple values separated by ",".

~~~~~~~~ {.xml}
<option name="port" value="80" values="80,8080,8888" enforcedvalues="true" 
        multivalued="true" delimiter="," />
~~~~~~~~ 

Use a multi-line description inside a CDATA section to preserve 
whitespace. Wrap the content in `<![CDATA[` and `]]>`:

~~~~~~~~ {.xml}
<option name='example'>
  <description><![CDATA[example option description

* this content will be rendered
* as markdown]]></description>
</option>
~~~~~~~~ 


#### valuesUrl JSON 

The data returned from the valuesUrl can be formatted as a list of values:

~~~~~~~~ {.json}
["x value","y value"]
~~~~~~~~ 

or as Name-value list:

~~~~~~~~ {.json .numberLines}
[
  {name:"X Label", value:"x value"},
  {name:"Y Label", value:"y value"},
  {name:"A Label", value:"a value"}
] 
~~~~~~~~ 

## dispatch 

     
The [job](#job) dispatch options. See the [Dispatcher options] for
general information.

*Nested elements*

[threadcount](#threadcount)

:    dispatch up to threadcount

[keepgoing](#keepgoing)

:    keep going flag

[rankAttribute](#rankattribute)

:    Name of the Node attribute to use for ordering the sequence of nodes (default is "nodename")

[rankOrder](#rankorder)

:    Order direction for node ranking. Either "ascending" or "descending" (default "ascending")

*Example*

~~~~~~~~ {.xml}
<dispatch>
  <threadcount>1</threadcount>
  <keepgoing>false</keepgoing>
  <rankAttribute>nodename</rankAttribute>
  <rankOrder>descending</rankOrder>
</dispatch>
~~~~~~~~ 

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
* ERROR

## nodefilters
     
The [job](#job) nodefilters options.

*Attributes*

excludeprecedence

:    boolean value: true or false

*Nested elements*

[filter](#filter)

:    node filter string

[include](#include)

:    include filter (deprecated)

[exclude](#exclude)

:    exclude filter (deprecated)


*Example*

~~~~~~~~ {.xml}
<nodefilters excludeprecedence="true">
  <filter>.*</filter>
</nodefilters>
~~~~~~~~ 


### filter

The filter string to select matching nodes.

The content of this element is the full node filter string. See [User Guide - Node Filters](../manual/node-filters.html).

### include

See [Include/exclude patterns](#includeexclude-patterns)

### exclude

See [Include/exclude patterns](#includeexclude-patterns)

### Include/exclude patterns 

The [nodefilters](#nodefilters) include and exclude patterns.  

**Note:** These elements are deprecated and will be removed in a later version of Rundeck.  Use the [filter](#filter) string.

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
* [Plugin step](#plugin-step)

A command can embed a [errorhandler](#errorhandler) to define
an action to run if the step fails.

A command can have a [description](#description) element to set the step description.

### errorhandler

Defines an action to handle an error in a [command](#command).

The contents of an `<errorhandler>` are exactly the same as for a 
[command](#command) except it cannot contain any errorhandler itself.

The different types of errorhandler steps are defined in different ways.

*Attributes*

`keepgoingOnSuccess`

:    true/false. (default false). If true, and the error handler succeeds, the workflow sequence will continue even if the workflow `keepgoing` is false.

See:

* [Script sequence step](#script-sequence-step)
* [Job sequence step](#job-sequence-step)
* [Plugin step](#plugin-step)

Example:

~~~~~~~~ {.xml}
<errorhandler>
   <exec>echo this is a shell command</exec>
</errorhandler>
~~~~~~~~ 

Inline script.  Note that using CDATA section will preserve linebreaks
in the script.  Simply put the script within a <code>script</code>
element:

~~~~~~~~ {.xml}
<errorhandler>
    <script><![CDATA[#!/bin/bash
echo this is a test
echo whatever
exit 2 ]></script>
</errorhandler>
~~~~~~~~ 


Script File:

~~~~~~~~ {.xml}
<errorhandler>
    <scriptfile>/path/to/a/script</scriptfile>
    <scriptargs>-whatever something</scriptargs>
</errorhandler>      
~~~~~~~~

Example job reference:

~~~~~~~~ {.xml}
<errorhandler >
    <jobref group="My group" name="My Job">
       <arg line="-option value -option2 value2"/>
    </jobref>
</errorhandler>      
~~~~~~~~ 

### description
 
Defines a description for a step. 

Example:

~~~~~~~~ {.xml}
<command>
   <exec>echo this is a shell command</exec>
   <description>Demonstrate echo command</description>
</command>
~~~~~~~~ 

### Script sequence step 

Script steps can be defined in three ways within a command element:

* Simple shell command using <code>exec</code> element.
* Embedded script using <code>script</code> element.
* Script file using <code>scriptfile</code> and <code>scriptargs</code> elements.

Example exec step:

~~~~~~~~ {.xml}
<command>
   <exec>echo this is a shell command</exec>
</command>
~~~~~~~~ 

Inline script.  Note that using CDATA section will preserve linebreaks
in the script.  Simply put the script within a <code>script</code>
element:

~~~~~~~~ {.xml}
<command>
    <script><![CDATA[#!/bin/bash
echo this is a test
echo whatever
exit 2 ]></script>
</command>
~~~~~~~~


Script File:

~~~~~~~~ {.xml}
<command >
    <scriptfile>/path/to/a/script</scriptfile>
    <scriptargs>-whatever something</scriptargs>
</command>      
~~~~~~~~ 

Script URL:

~~~~~~~~ {.xml}
<command >
    <scripturl>http://example.com/path/to/a/script</scripturl>
    <scriptargs>-whatever something</scriptargs>
</command>      
~~~~~~~~ 

#### Script Interpreter

When using `<script>`, or `<scriptfile>`, you can declare an interpreter to use to execute the script and its args.

Add `<scriptinterpreter>` to the `<command>`:

~~~~~~~~ {.xml}
<command >
    <scriptinterpreter>sudo -u usera</scriptinterpreter>
    <scripturl>http://example.com/path/to/a/script</scripturl>
    <scriptargs>-whatever something</scriptargs>
</command>
~~~~~~~~ 

This will be executed effectively with this commandline:

~~~~~~~~ {.bash}
sudo -u usera script.sh -whatever something
~~~~~~~~ 

If the filename and arguments need to be quoted when passed to the interpreter, you can declare `argsQuoted='true'`:

~~~~~~~~ {.xml}
<command >
    <scriptinterpreter argsquoted='true'>sudo -u usera sh -c </scriptinterpreter>
    <scripturl>http://example.com/path/to/a/script</scripturl>
    <scriptargs>-whatever something</scriptargs>
</command>
~~~~~~~~ 

This will execute as:

~~~~~~~~ {.bash}
sudo -u usera sh -c 'script.sh -whatever something'
~~~~~~~~ 

### Job sequence step

Define a [jobref](#jobref) element within the [command](#command) element

#### jobref 

*Attributes*

name

:    the job name

group

:    the group name

nodeStep

:    `true/false` whether the Job reference step should run for each node


*Nested elements*

Optional "arg" element can be embedded:

##### arg

:    option arguments to the script or job

Example passing arguments to the job:

~~~~~~~~ {.xml}
<command >
    <jobref group="My group" name="My Job">
       <arg line="-option value -option2 value2"/>
    </jobref>
</command>      
~~~~~~~~ 

If `nodeStep` is set to "true", then the Job Reference step will operate as a *Node Step* instead of the
default.  As a *Node Step* it will execute once for each matched node in the containing Job workflow, and
can use node attribute variable expansion in the arguments to the job reference.

#### nodefilters (jobref)

The node filters to override for the [jobref](#jobref).

*Nested elements*

[filter](#filter)

:    node filter string. See [User Guide - Node Filters](../manual/node-filters.html).

Example:

~~~ {.xml}
<jobref group="My group" name="My Job">
  <dispatch>
    <threadcount>1</threadcount>
    <keepgoing>false</keepgoing>
    <rankAttribute>nodename</rankAttribute>
    <rankOrder>descending</rankOrder>
  </dispatch>
  <nodefilters>
    <filter>tags: production+appserver</filter>
  </nodefilters>
</jobref>
~~~

#### dispatch (jobref)

The dispatch options to override for the [jobref](#jobref).

The content is the same as for the [job dispatch](#dispatch) section.

### Plugin step

There are two types of plugin steps that can be defined: Node steps, and workflow steps.

Define either one within the [command](#command) element:

* [node-step-plugin](#node-step-plugin)
* [step-plugin](#step-plugin)

Both have the following contents:

*Attributes*

type

:    The plugin provider type identifier

*Nested elements*

Optional 'configuration' can be embedded containing a list of 'entry' key/value pairs:

[configuration](#configuration)

:    Defines plugin configuration entries

[entry](#entry)

:    Defines a key/value pair for the configuration.


Example node step plugin definition:

~~~~~~~~ {.xml}
<command>
    <node-step-plugin type="my-node-step-plugin">
       <configuration>
        <entry key="someconfig" value="a value"/>
        <entry key="timout" value="2000"/>
       </configuration>
    </node-step-plugin>
</command> 
~~~~~~~~ 

Example workflow step plugin definition:

~~~~~~~~ {.xml}
<command>
    <step-plugin type="my-step-plugin">
       <configuration>
        <entry key="repeat" value="12"/>
        <entry key="debug" value="true"/>
       </configuration>
    </step-plugin>
</command>     
~~~~~~~~ 

#### node-step-plugin

Defines a plugin step that executes for each node.

#### step-plugin

Defines a plugin step that executes once in a workflow.

#### configuration

Contains the key/value pair entries for plugin configuration, within a [node-step-plugin](#node-step-plugin) or [step-plugin](#step-plugin).

#### entry

Defines a key/value pair within a [configuration](#configuration).

*Attributes*:

key

:    Key for the pair

value

:    Textual value

## notification 

Defines email, webhook or plugin notifications for Job success and failure, with in a
[job](#job) definition.

*Nested elements*

[onsuccess](#onsuccess)

:    define notifications for success result

[onfailure](#onfailure)

:    define notifications for failure/kill result

[onstart](#onstart)

:    define notifications for job start

*Example*

~~~~~~~~ {.xml}
<notification>
    <onfailure>
        <email recipients="test@example.com,foo@example.com" />
    </onfailure>
    <onsuccess>
        <email recipients="test@example.com" />
        <webhook urls="http://example.com?id=${execution.id}" />
   </onsuccess>
    <onstart>
        <plugin type="MyPlugin">
          <configuration>
            <entry key="customkey" value="customvalue"/>
          </configuration>
        </plugin>
   </onstart>
</notification>      
~~~~~~~~ 

 
### onsuccess 

Embed an [email](#email) element to send email on success, within
[notification](#notification).

Embed an [webhook](#webhook) element to perform a HTTP POST to some URLs, within
[notification](#notification).

Embed an [plugin](#plugin) element to perform a custom action, within
[notification](#notification).

### onfailure 

Embed an [email](#email) element to send email on failure or kill,
within [notification](#notification).

Embed an [webhook](#webhook) element to perform a HTTP POST to some URLs, within
[notification](#notification).

Embed an [plugin](#plugin) element to perform a custom action, within
[notification](#notification).

### onstart 

Embed an [email](#email) element to send email on failure or kill,
within [notification](#notification).

Embed an [webhook](#webhook) element to perform a HTTP POST to some URLs, within
[notification](#notification).

Embed an [plugin](#plugin) element to perform a custom action, within
[notification](#notification).

### email 

Define email recipients for Job execution result, within [onsuccess][], [onfailure][] or [onstart][].

[onsuccess]: #onsuccess
[onfailure]: #onfailure
[onstart]: #onstart

*Attributes*

recipients

:    comma-separated list of email addresses

*Example*

            <email recipients="test@example.com,dev@example.com" />

### webhook

Define URLs to submit a HTTP POST to containing the job execution result, within [onsuccess][], [onfailure][] or [onstart][].

[onsuccess]: #onsuccess
[onfailure]: #onfailure
[onstart]: #onstart

*Attributes*

urls

:   comma-separated list of URLs


*Example*

~~~~~~~~ {.xml }
<webhook urls="http://server/callback?id=${execution.id}&status=${execution.status}&trigger=${notification.trigger}"/>
~~~~~~~~ 

* For more information about the Webhook mechanism used, see the chapter [Integration - Webhooks](../manual/jobs.html#webhooks).

### plugin

Defines a configuration for a plugin to perform a Notification, within [onsuccess][], [onfailure][] or [onstart][].

[onsuccess]: #onsuccess
[onfailure]: #onfailure
[onstart]: #onstart

*Attributes*

type

:    The plugin provider type identifier

*Nested elements*

Optional 'configuration' can be embedded containing a list of 'entry' key/value pairs:

[configuration](#configuration-1)

:    Defines plugin configuration entries

[entry](#entry-1)

:    Defines a key/value pair for the configuration.


Example notification plugin definition:

~~~~~~~~ {.xml}
<onstart>
    <plugin type="my-notification-plugin">
       <configuration>
        <entry key="someconfig" value="a value"/>
        <entry key="timout" value="2000"/>
       </configuration>
    </plugin>
</onstart> 
~~~~~~~~ 

#### configuration

Contains the key/value pair entries for plugin configuration, within a [plugin](#plugin) notification definition.

#### entry

Defines a key/value pair within a [configuration](#configuration-1).

*Attributes*:

key

:    Key for the pair

value

:    Textual value


# SEE ALSO

`[rd-jobs](../man1/rd-jobs.html)`

The Rundeck source code and all documentation may be downloaded from
<https://github.com/rundeck/rundeck/>.
