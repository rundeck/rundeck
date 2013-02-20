% JOB-YAML-V12(5) Rundeck User Manuals | Version 1.2
% Greg Schueler
% February 25, 2011

# NAME

job-yaml-v12 - The 'job' YAML file declares job entries for Rundeck.

## Loading and unloading

This file can be batch loaded via *rd-jobs* load command:

    rd-jobs load --file /path/to/jobs.yaml -F yaml

Rundeck job definitions can be dumped and saved to a file via
rd-jobs list command:

    rd-jobs list --file /tmp/jobs.yaml -F yaml

## Structure

The YAML document can contain multiple Job definitions, in a sequence:

    - # job 1
      name: ...
    - # job 2
      name: ...

Each Job definition is a Map consisting of some required and some optional entries, as listed below.

## Job Map Contents

Each Job definition requires these values:

`name`

:    the job name

`uuid`

:    Unique UUID

`description`

:    the job description (can be blank)

`project`

:    the Project name

`loglevel`

:    the loglevel to use for the job, the value must be one of:

    * `DEBUG`
    * `VERBOSE`
    * `INFO`
    * `WARN`
    * `ERR`

[`sequence`](#sequence)

:    The workflow sequence definition

A minimal job definition example:

    name: job name
    description: ''
    project: project1
    loglevel: INFO
    sequence: 
      - exec: a command

In addition, these optional entries can be present:

`group`

:    Job group name

`multipleExecutions`

:    'true/false': if true, the job can have more than one execution at once.

[`options`](#options)

:    Set of Options for the Job

[`schedule`](#schedule)

:    Job schedule

[`nodefilters`](#nodefilters)

:    Node filter definition

[`notification`](#notification)

:    Job result notifications

*Note:* The UUID can be set manually (if
you are writing the job definition from scratch), or will be assigned at job
creation time by the Rundeck server using a random UUID.  This string should be 
as unique as possible if you set it manually.

This identifier is used to uniquely identify jobs when ported between Rundeck
instances.

### Sequence

This defines the Workflow options and execution sequence.

Example:

    sequence:
      keepgoing: true
      strategy: node-first
      commands: 
      - exec: ...
      - script: ...
        args: ...
      - scriptfile: ...
        args:
      - scripturl: ...
        args:
      - jobref:
          name: jobname
          group: group
          args: args

The sequence has these required entries:

`keepgoing`

:    "true/false" - whether the sequence should keep going if an error occurs

`strategy`

:    "node-first" or "step-first".  Determines the strategy for executing the sequence across a set of nodes.  See the [Rundeck User Manual](manual/job-workflows.html#workflow-control-settings) for more info.

`commands`

:    This is a Sequence of:
    * One or more [Command Definitions](#command)

### Command

Each command in the [Sequence](#sequence) can be of these different types:

* [Simple command execution entry](#simple-command-entry)
* [Script execution entry](#script-execution-entry)
* [Script file execution entry](#script-file-execution-entry)
* [Job Reference entry](#job-reference-entry)

#### Simple Command Entry

This [Command](#command) consists of a single entry:

`exec`

:    the command to execute

#### Script Execution Entry

This [Command](#command) executes the script content specified.

`script`

:     The script content.  It is useful to use the YAML "literal" scalar syntax shown below

`args`

:     Optional string defining arguments to pass to the script.

Example:

     - script: |-
        #!/bin/bash

        echo this is a script
        echo this is option value: @option.test@
      args: arguments passed to the script

#### Script File Execution Entry

This [Command](#command) executes a script file stored on the server.

`scriptfile`

:    path to the script file

`args`

:     optional arguments to the script

Example:

    - scriptfile: /path/to/script
      args: arguments to script

#### Script URL Execution Entry

This [Command](#command) downloads a script file from a URL and executes it.

`scripturl`

:    URL to the script file

`args`

:     optional arguments to the script

Example:

    - scripturl: http://example.com/path/to/script
      args: arguments to script

#### Job Reference Entry

This [Command](#command) executes another Rundeck Job.

`jobref`

:    map  consisting of these entries:

    `name`

    :    Name of the Job

    `group`

    :    Group of the Job (optional)

    `args`

    :    Arguments to pass to the job when executed

Example:

    - jobref:
        group: test
        name: simple job test
        args: args for the job

### Options

Options for a job can be specified with a map. Each map key is the name of the option, and the content is a map defining the [Option](#option).

    options:
      optname1:
        [definition..]
      optname2:
        [definition..]

### Option

An option definition has no required entries, so it could be empty:

    myoption: {}

Optional map entries are:

`description`

:    description of the option

`value`

:    a default value for the option

`values`

:    A set of possible values for the option. This must be a YAML Sequence of strings.

`required`

:    "true/false" - whether the option is required or not

`enforced`

:    "true/false" - whether the option value must be one of the specified possible values

`regex`

:    A regular expression defining what option values are acceptable

`valuesUrl`

:    A URL to an endpoint that will return a JSON-formatted set of values for the option.

`multivalued`

:    "true/false" - whether the option supports multiple input values

`delimiter`

:    A string used to conjoin multiple input values.  (Required if `multivalued` is "true")

`secure`

:   "true/false" - whether the option is a secure input option. Not compatible with "multivalued"

Example:

    test:
      required: true
      description: a test option
      value: dvalue
      regex: ^[abcd]value$
      values:
      - avalue
      - bvalue
      - cvalue
      multivalued: true
      delimiter: ','

#### valuesUrl JSON 

The data returned from the valuesUrl can be formatted as a list of values:

    ["x value","y value"]

or as Name-value list:

    [
      {name:"X Label", value:"x value"},
      {name:"Y Label", value:"y value"},
      {name:"A Label", value:"a value"}
    ] 

* See the [Rundeck Guide](manual/job-options.html#remote-option-values) for more info.

### Schedule

Define a schedule for repeated execution of the Job.  The schedule can be defined as a Crontab formatted string, or as individual components.  The individual components support Crontab syntax.

* `crontab`: The crontab string, e.g. `"0 30 */6 ? Jan Mon *"`

Or use a structure of explicit components. All of these are optional, but likely you want to change them:

`time`

:    a map containing:

    `seconds`

    :    seconds value (default: "0")

    `minute`

    :    minutes value (default: "0")

    `hour`

    :    hour value (default: "0")

`month`

:    Month value (default: "*")

`year`

:    Year value (default "*")

`dayofmonth`

:    day of month value. (mutually exclusive with `weekday`)

`weekday`

:    Map containing:

    `day`

    :    Weekday value. (mutually exclusive with `dayofmonth`) (default: "*") Numerical values are 1-7 for Sunday-Saturday.

Example using crontab string:

    schedule:
      crontab: '0 30 */6 ? Jan Mon *'

Example using structure:

    schedule:
      time:
        hour: '05'
        minute: '01'
        seconds: '0'
      month: APR,MAR,MAY
      year: '*'
      weekday:
        day: FRI,MON,TUE

### Nodefilters

Specifies the Nodes to use for the job,  and the node-dispatch options.  Contains the following entries:

`dispatch`

:    a Map containing:

    `keepgoing`

    :    "true/false" - whether to keepgoing on remaining nodes if a node fails

    `excludePrecedence`

    :    "true/false" (default "true") - determines precedence for filters

    `threadcount`

    :    Number of threads to use for parallel dispatch (default "1")
    
    `rankAttribute`

    :    Name of the Node attribute to use for ordering the sequence of nodes (default is the node name)

    `rankOrder`

    :    Order direction for node ranking. Either "ascending" or "descending" (default "ascending")
    
The `nodefilters` must also contain ONE of `include` or `exclude` filter specifiers.

`include`/`exclude`

:    A Map containing filter entries:

    `hostname`

    :    Hostname filter

    `name`

    :    Node name filter

    `tags`

    :    Tags filter.  Supports boolean operators AND ("+") and OR (",").

    `os-name`

    :    OS name filter

    `os-family`

    :    OS Family filter

    `os-arch`

    :    OS Arch filter

    `os-version`

    :    OS Version filter

Example:

    nodefilters:
      dispatch:
        threadcount: 1
        keepgoing: false
        excludePrecedence: true
        rankAttribute: rank
        rankOrder: descending
      include:
        tags: web
        name: web-.*
      exclude:
        os-family: windows

### Notification

Defines result notification for the job.  You can include one or both of `onsuccess` or `onfailure` notifications. Each type of notification can include a list of email addresses and/or a list of URLs to use as a webhook.

`onsuccess`/`onfailure`

:    A Map containing either or both of:

    `recipients`

    :    A comma-separated list of Email addresses
    
    `urls`
    
    :    A comma-separated list of URLs to use as webhooks

Example:

    notification:
      onfailure:
        recipients: tom@example.com,shirley@example.com
      onsuccess:
        urls: 'http://server/callback?id=${execution.id}&status=${execution.status}&trigger=${notification.trigger}'

* For more information about the Webhook mechanism used, see the chapter [Integration - Webhooks](manual/jobs.html#webhooks).

# SEE ALSO

`rd-jobs` (1).

<http://yaml.org/>

The Rundeck source code and all documentation may be downloaded from
<https://github.com/dtolabs/rundeck/>.
