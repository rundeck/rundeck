% Job Options
% Alex Honor; Greg Schueler
% November 20, 2010

Any command or script can be wrapped as a Job. Creating a Job for
every use case will proliferate a large number of Jobs differing only
by how the Job calls the scripts. These
differences are often environment or application version
related. Other times only the person running the Job can provide the
needed information to run the Job correctly. 

Making your scripts and commands data driven, will also make them
more generic and therefore, reusable in different contexts. Rather than
maintain variations of the same basic process, letting Jobs be driven
by a model of options from externally provided data will lead to
better abstraction and encapsulation of your process.

Rundeck Jobs can be configured to prompt a user for input by defining
one or more named *options*. An *option* models a named parameter that
can be required or optional and include a range of choices that will
be presented to the user when the Job is run.

Users supply options by typing in a value or selecting from a menu
of choices. A validation pattern ensures input complies to the
option requirement. Once chosen, the value chosen for the option is
accessible to the commands called by the Job.

Option choices can be modeled as a static set or from a dynamic
source. Static choices can be modeled as a comma separated list in the
job definition. When option values must be
dynamic, the Job can be defined to use a URL to retrieve option data
from an external source. Enabling Jobs to access external sources via
URL opens the door to integrating Rundeck with other tools and
incorporating their data into Job workflows. 

## Prompting the user

The obvious effect from defining Job options is their appearance to
the user running the Job. Users will be presented a page called "Choose
Execution Options..." where input and menu choices must be configured.

Command line users executing Jobs via the `run` shell
tool also will specify options as an argument string.

It is worth spending a moment to consider how options become
part of the user interface to Jobs and give some thought to this next
level of procedure formalization.

* Naming and description convention: Visualize how the user will read
  the option name and judge its purpose from the description you supply.
* Required options: Making an option required means the Job will fail
  if a user leaves it out.
* Input restrictions and validation: If you need to make the option
  value be somewhat open ended consider how you can create
  safeguards to control their choice.

## Input Types

Option Input Types define how the option is presented in the GUI, and how it is used when the Job executes.

Input types:

* "Plain" - a normal option which is shown in clear text
* "Secure" - a secure option which is obscured at user input time, and the value of which is not stored in the database
*  "Secure Remote Authentication" - a secure option which is used only for remote authentication and is not exposed in scripts or commands.

## Secure Options

Options can be marked as Secure, to show a password prompt in the GUI, instead of a normal text field or drop down menu.  Secure option values are not stored with the Execution as are the other option values.

There are two types of Secure options:

* Secure - these option values are exposed in scripts and commands.
* Secure Remote Authentication - these option values *are not* exposed in scripts and commands, and are only used by Node Executors to authenticate to Nodes and execute commands.

Secure Options do not support Multi-valued input. 

Secure Options cannot be used as authentication input to Node Executors, you must use a Secure Remote Authentication option described below.

**Important Note**

"Secure" option values are not stored in the Rundeck database when the Job is executed, but the value that is entered 
is exposed to use in scripts and commands.  Make sure you acknowledge these security implications before using them. Secure options are available for use in scripts and command like any other option value: 

* as plaintext arguments using `${option.name}`
    * Using the option value as an argument to a command could expose the plaintext value in the system process table
* as plaintext environment variables in remote and local script execution as `$RD_OPTION_NAME`
    * Local and possibly remote scripts may be passed this value into their environment
* as plaintext tokens expanded in remote scripts as `@option.name@`.
    * Inline Script workflow steps that contain a token expansion will be expanded into a temporary file, and the temp file will contain the plaintext option value.

Note: that when passed as arguments to Job References, they can only be passed as the value of another Secure option.  See [Using Secure Options with Job References](#using-secure-options-with-job-references).

### Secure Remote Authentication Options

The built-in [SSH Provider](plugins.html#ssh-provider) for node execution allows using passwords for SSH and/or Sudo authentication mechanisms, and the passwords are supplied by Secure Remote Authentication Options defined in a Job.

Secure Remote Authentication Options have some limitations compared to Plain and Secure options:

* The values entered by the user are not available for normal script and command option value expansion. This means that they can only be used for the purposes of the Remote Authentication.

### Using Secure Options with Job References

When you [define a Job Reference step in a workflow](job-workflows.html#job-reference-step), you can specify the arguments that are passed to it. You can pass Secure Option values and Secure Remote Authentication Option values from a top-level job to a Job Reference, but option values *cannot be passed into another option of a different type*. So a parent job can only pass option values to the Job reference if the option type is the same between the jobs.

This constraint is to maintain the security design of these options:

1. Secure options should not to be stored in the Rundeck execution database, so must not be used as plain option values.
2. Secure Remote Authentication options should not be used in scripts/commands, so must not be used as Secure or Plain option values.

As an example, here is are two jobs, Job A and Job B, which define some options:

* Job A
    * Option "plain1" - Plain
    * Option "secure1" - Secure
    * Option "auth1" - Secure remote authentication
* Job B
    * Option "plain2" - Plain
    * Option "secure2" - Secure
    * Option "auth2" - Secure remote authentication

If Job A defines a Job reference to call Job B, then the only valid mapping is shown below:

* plain1 -> plain2
* secure1 -> secure2
* auth1 -> auth2

So the arguments for the Job Reference might look like this:

    -plain2 ${option.plain1} -secure2 ${option.secure1} -auth2 ${option.auth1}

Note: If you define arguments in the wrong manner, then the Secure and Secure Remote Authentication options will not be set when the Job reference is called.  Plain options will behave the way they do in Command or Script arguments, and be left as-is as uninterpreted property references.

## Options editor

Options can be created for any stored Job. The Job edit page contains
an area displaying a summary to existing options and a link to add new
ones or edit existing ones.

![Add option link](../figures/fig0501.png)

The option summary shows each option and its default value if it defines
them.

Clicking the  "edit" link opens the options editor. 

![Option editor](../figures/fig0503.png)

The options editor displays an expanded summary for each defined
option. Each option is listed with its usage summary,
description, values list and any restrictions. Pressing the "Add an
option" link will open a form to define a new parameter. Pressing the
"Close" link will collapse the options editor and return back to the
summary view.

Moving the mouse over any row in the options editor reveals links to
delete or edit the highlighted option. Pressing the remove icon will
display a prompt confirming you want to delete that option from the Job.
Clicking the "edit" link opens a new form that lets you modify all
aspects of that option.

Options can also be defined as part of a job definition and later
loaded to the Rundeck server. See [job-v20(5)](../manpages/man5/job-v20.html)(XML) and [job-yaml-v12(5)](../manpages/man5/job-yaml-v12.html)(YAML) and 
[rd-jobs(1)](../manpages/man1/rd-jobs.html) manual
pages if you prefer using an textual Job definition.

## Defining an option

New options can be defined by pressing the "Add an option" link while
existing ones can be changed by pressing their "edit" link.

![Option edit form](../figures/fig0502.png)

The option definition form is organized into several areas:

Identification

:    Here you provide the option's name and description. The name
     becomes part of acceptable arguments to the Job while the
     description will be provided as help text to users running the Job.
     
     The Default Value will be pre-selected in the GUI when the option is presented.

Input Type

:   Choose between "Plain", "Secure" and "Secure Remote Authentication". For input types other than "Plain", the multi-valued option will be disabled.

Allowed values

:    Allowed values provide a model of possible choices.
     This can contain a static list of values or a URL to a server
     providing option data. Values can be specified as a comma
     separated list as seen above but can also be requested from an
     external source using a "remote URL" [See below](job-options.html#remote-option-values).
     

Restrictions

:    Defines criteria on which input to accept or present. Option
     choices can be controlled using the "Enforced from values"
     restriction. When set "true", Rundeck will only present a
     popup menu. If set "false", a text field will also be presented. 
     Enter a regular expression in the "Match Regular Expression"
     field the Job will evaluate when run.

Requirement

:    Indicates if the Job can only run if a choice is provided for
     that Option. Choosing "No" states the option is not required
     Choose "Yes" to state the option is required.
     
     If a Default Value is set for the option, then this value will automatically be set for the option if it is Required, even if not specified among the arguments when executing a job via the command-line or API.

Multi-valued

:    Defines if the user input can consist of multiple values. Choosing "No" states that only a single value can chosen as input. Choosing "Yes" states that the user may select multiple input values from the Allowed values and/or enter multiple values of their own.  The delimiter string will be used to separate the multiple values when the Job is run.

Once satisfied with the option definition, press the "Save" button to
add it to the Job definition. Pressing the "Cancel" button will
dismiss the changes and close the form.

## Remote option values

A model of option values can be retrieved from an external source.
When the `valuesUrl` is specified for an Option, then the model of
allowed values is retrieved from the specified URL. 

This is useful in a couple of scenarios when Rundeck is used to 
coordinate process that depend on other systems:

* Deploying packages or artifacts produced by a build or CI server, e.g. Hudson.
    * A list of recent Hudson build artifacts can be imported as Options data, so that a User can pick an appropriate package name to deploy from a list.
* Selecting from a set of available environments, defined in a CMDB
* Any situation in which input variables for your Jobs must be selected from some set of values produced by a different system.

See [Chapter 9 - Option Model Provider](job-options.html#option-model-provider).

## Script usage

Option values can be passed to scripts as an argument or referenced
inside the script via a named token. Each option value is defined in the Options context variables as `option.NAME`.

See the [Job Workflows - Context Variables](job-workflows.html#context-variables) Section.

**Example:**

A Job named "hello" and has an option named "message".

The "hello" Job option signature would be: `-message <>`.

![Option usage](../figures/fig0504.png)

The arguments passed to the script are defined as `${option.message}`.

Here's the content of this simple script. 

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ {.bash}
    #!/bin/sh    
    echo envvar=$RD_OPTION_MESSAGE ;# read from environment
    echo args=$1                   ;# comes from argument vector
    echo message=@option.message@  ;# replacement token
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 

When the user runs the "hello" job they will be prompted for the
"message" value.

![Option entered](../figures/fig0505.png)

Let's assume they entered the word "howdy" in response. 
The output of the Job will be:

    envar=howdy
    args=howdy    
    message=howdy    

It's important to know what happens if the option isn't set. This can
happen if you define an option that is not required and do not give it
a default value. 

Let's imagine the Job was run without a message option supplied, the
output would look like this:

    envar=
    args=
    message=@option.message@

Here are some tips to deal with this possibility:

Environment variable:

:    As a precaution you might test existence for the variable and
     perhaps set a default value.
     To test its existence you might use: 

         test -s  $RD_OPTION_NAME

     You might also use a Bash feature that tests and defaults it to a
     value:

         ${RD_OPTION_NAME:=mydefault} 

Replacement token	 

:    If the option is unset the token will be left alone inside the
     script. You might write your script a bit more defensively and
     change the implementation like so:

        message=@option.message@
        atsign="@"
        if [ "$message" == "${atsign}option.message${atsign}" ] ; then
           message=mydefault
        fi

## Calling a Job with options

Jobs can be invoked from the command line using the `run`
shell tool or as a step in another Job's workflow.

The format for specifying options is `-name value`.

Using the `run` command pass them after the double hyphen:

    run -i jobId -- -paramA valA -paramB valB
    run -j group/name -p project -- -paramA valA -paramB valB

Inside an XML definition, insert them as an `arg` element:

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ {.xml}
<command>
    <jobref group="test" name="other tests">
        <arg line="-paramA valA -paramB valB"/>
    </jobref>
</command>
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 

Consult the [run(1)](../manpages/man1/run.html) and [job-v20(5)](../manpages/man5/job-v20.html) manual pages for additional
information.


## Option model provider

The Option model provider is a mechanism to allow the Options defined for a Job to have some of the possible input values provided by a remote service or database.  

Option model providers are configured on a per-Option basis (where a Job may have zero or more Options).

### Requirements ###

1. Options model data must be [JSON formatted](http://www.json.org).
2. It must be accessible via HTTP(S) or on the local disk for the Rundeck server.
3. It must be in one of two JSON structures, *either*:
    * An array of string values
    * OR, an array of Maps, each with two entries, `name` and `value`.

### Configuration ###

Each Option entry for a Job can be configured to get the set of possible values from a remote URL.  If you are authoring the Jobs via [job.xml file format](../manpages/man5/job-v20.html#option), simply add a `valuesUrl` attribute for the `<option>`.  If you are modifying the Job in the Rundeck web GUI, you can entry a URL in the "Remote URL" field for the Option.

e.g.:

    <option valuesUrl="http://site.example.com/values.json" ...

*Note*: File URL scheme is also acceptable (e.g, `file:/path/to/job/options/optA.json`).

The value data must be returned in JSON data format described below.

### JSON format

Three styles of return data are supported: simple list, simple object, and a name/value list. For a simple list, the list values will be displayed in a pop-up list when running the Job.  If a simple object or name/value pairs are returned, then the `name` will be displayed in the list, but the `value` will be used as the input.

*Examples*

Simple List:

    ["x value for test","y value for test"]

This will populate the select menu with the given values.

Simple Object:

    { "Name": "value1", "Name2":"value2" }

This will populate the select menu to show the names and use the values.

Name Value List:
 
    [
      {name:"X Label", value:"x value"},
      {name:"Y Label", value:"y value"},
      {name:"A Label", value:"a value"}
    ] 

### Cascading Remote Options

Cascading options allow an option's Remote values URL to embed the values
entered by the user for other options when executing a Job.  When the user
enters or selects a value for one of the other options, then the remote JSON is
refreshed for the current option.

This provides a mechanism for declaring hierarchical or dependent sets of option
values.

E.g. if you wanted one option to choose a "repository", and  another option to
select a specific "branch" within that repository.  Define your option provider
to respond correctly based on the  selected "repository" value, and define your
Remote option URL to include a reference to the "repository" option value. The
Rundeck GUI will then reload the JSON values from the remote URL and insert the
correct value of the "repository" option when loading the "branch" option
values. If the user changes the selected repository, then the branch values will
be automatically refreshed.

You can declare a
dependency of one option to another by embedding property references within the
remote values URL.  The property reference is of the form
`${option.[name].value}`.  If you declare an option with a remote values URL
like "http://server/options?option2=${option.option2.value}", then that option
will depend on the value of the "option2" option.

In the GUI, when the options are loaded, option2 will be shown first, and the
remote values for option1 will only be loaded once a value has been selected for
option2, and the value will be placed in the URL when it is loaded.

If an option has dependencies on other options that do not have a value set,
then the  embedded references will evaluate to "" (empty string) when loading
the URL.

If an option has dependencies on other options and the remote values [JSON 
data](#json-data) is empty (empty list or empty object), then the option shown in
the GUI will indicate that the user should select values for the necessary
options.  This allows Option model providers to indicate that some or all of the
dependent option values are necessary for the current option before showing the
input for the option.

It is possible to have dependencies on more than one option, and any change to
one of the dependencies will cause the option values to be reloaded from the
remote URL.

Note: It is also possible to declare a cycle of dependencies between option
values, which will cause the automatic reloading to be disabled.  In this case
the user must manually click the reload button to reload the option values if a
dependency has changed.

### Variable expansion in remote URLs

The URL declared for the "valuesUrl" can embed variables which will be
filled with certain job context items when making the remote request. This
helps make the URLs more generic and contextual to the Job.

Two types of expansions are available, Job context, and Option
context.

To include job information in the URL, specify a variable of the form
${job._property_}.

Properties available for Job context:

* `name`: Name of the Job
* `group`: Group of the Job
* `description`: Job description
* `project`: Project name
* `argString`: Default argument string for a job

To include Option information in the URL, specify a variable of the
form ${option._property_}:

Properties available for Option context:

* `name`: Name of the current option


To include [Cascading remote option](#cascading-remote-options) values information in the URL, specify a variable of the
form ${option._name_.value}:

* `option.[name].value`: substitutes the selected value of another option by name. If the option is not set, then a blank string ("") will be substituted.

*Examples*

    http://server.com/test?name=${option.name}

Passes the option name as the "name" query parameter to the URL.

    http://server.com/test?jobname=${job.name}&jobgroup=${job.group}

Passes the job name and group as query parameters.
    
    http://server.com/branches?repository=${option.repository.value}

Passes the value of the selected "repository" option, or "" (blank) if it is not set. This option becomes
a dependent of the "repository" option, and if the "repository" value changes, the remote option values
for this option will be reloaded.

### Remote request failures

If the request for the remote option values fails, then the GUI form
will display a warning message:

![](../figures/fig0901.png)
    
In this case, the option will be allowed to use a textfield to set the value.

### Implementations and Examples ###

The following two sections describe examples using simple CGI scripts
that act as option model providers.
 
#### Hudson artifacts option provider 

An end-to-end release process often requires obtaining build artifacts
and publishing them to a central repository for later distribution.
A continuous integration server like [Hudson] makes identifying the
build artifacts a simple Job configuration step. The [Hudson API]
provides a network interface to obtain the list of artifacts from
successful builds via a simple HTTP GET request.

Acme builds its artifacts as RPMs and has configured their build job
to identify them. The operations team wants to create Jobs that would
allow them to choose a version of these artifacts generated by the
automated build.

A simple CGI script that requests the information from Hudson and then
generates a [JSON] document is sufficient to accomplish this. The CGI
script can use query parameters to specify the Hudson server, hudson job
and artifact path. Job writers can then specify the parameterized URL
to the CGI script to obtain the artifacts list as an options model
and present the results as a menu to Job users.

The code listing below shows the the CGI script essentially does a
call to the [curl] command to retrieve the XML document
containing the artifacts information and then parses it using
[xmlstarlet].
 
File listing: hudson-artifacts.cgi
 
    #!/bin/bash
    # Requires: curl, xmlstarlet
    # Returns a JSON list of key/val pairs
    #
    # Query Params and their defaults
    hudsonUrl=https://build.acme.com:4440/job
    hudsonJob=ApplicationBuild
    artifactPath=/artifact/bin/dist/RPMS/noarch/
    
    echo Content-type: application/json
    echo ""
    for VAR in `echo $QUERY_STRING | tr "&" "\t"`
    do
      NAME=$(echo $VAR | tr = " " | awk '{print $1}';);
      VALUE=$(echo $VAR | tr = " " | awk '{ print $2}' | tr + " ");
      declare $NAME="$VALUE";
    done

    curl -s -L -k $hudsonUrl/${hudsonJob}/api/xml?depth=1 | \
      xmlstarlet sel -t -o "{" \
        -t -m "//build[result/text()='SUCCESS']" --sort A:T:L number  \
        -m . -o "&quot;Release" -m changeSet/item -o ' ' -v revision -b \
        -m . -o ", Hudson Build " -v number -o "&quot;:" \
        -m 'artifact[position() = 1]' -o "&quot;" -v '../number' -o $artifactPath -o "{" -b \
        -m 'artifact[position() != last()]' -v 'fileName' -o "," -b \
        -m 'artifact[position() = last()]' -v 'fileName' -o "}&quot;," \
        -t -o "}"

After deploying this script to a CGI enabled directory on the
operations web server, it can be tested directly by requesting it using `curl`.

    curl -d "hudsonJob=anvils&artifactPath=/artifact/bin/dist/RPMS/noarch/" \
        --get http://opts.acme.com/cgi/hudson-artifacts.cgi

The server response should return JSON data resembling the example below:

    [ 
      {name:"anvils-1.1.rpm", value:"/artifact/bin/dist/RPMS/noarch/anvils-1.1.rpm"}, 
      {name:"anvils-1.2.rpm", value:"/artifact/bin/dist/RPMS/noarch/anvils-1.2.rpm"} 
    ]	

Now in place, jobs can request this option data like so:

     <option name="package" enforcedvalues="true" required="true"
        valuesUrl="http://ops.acme.com/cgi/hudson-artifacts.cgi?hudsonJob=anvils"/> 

The Rundeck UI will display the package names in the menu and once
selected the Job will have the path to the build artifact on the
Hudson server.

[Hudson]: http://hudson-ci.org/
[Hudson API]: http://wiki.hudson-ci.org/display/HUDSON/Remote+access+API
[JSON]: http://www.json.org/

#### Yum repoquery option model provider

[Yum] is a great tool for automating [RPM] package management. With Yum,
administrators can publish packages to the repository and then use the
yum client tool to automate the installation of packages along with
their declared dependencies. Yum includes a command
called [repoquery] useful for
querying Yum repositories similarly to rpm queries.

Acme set up their own Yum repository to distribute application release
packages. The Acme administrator wants to provide an option model to Jobs that
need to know what packages provide a given capability.

The code listing below shows it is a simple wrapper around the
repoquery command that formats the results as JSON data.

File listing: yum-repoquery.cgi
    
    #!/bin/bash
    # Requires: repoquery
    # 
    # Query Params and their defaults
    repo=acme-staging
    label="Anvils Release"
    package=anvils
    max=30
    #
    echo Content-type: application/json
    echo ""
    for VAR in `echo $QUERY_STRING | tr "&" "\t"`
    do
      NAME=$(echo $VAR | tr = " " | awk '{print $1}';);
      VALUE=$(echo $VAR | tr = " " | awk '{ print $2}' | tr + " ");
      declare $NAME="$VALUE";
    done

    echo '{'
    repoquery --enablerepo=$repo --show-dupes \
      --qf='"${label} %{VERSION}-%{RELEASE}":"%{NAME}-%{VERSION}-%{RELEASE}",' \
      -q --whatprovides ${package} | sort -t - -k 4,4nr | head -n${max}
    echo '}'

After deploying this script to the CGI enabled directory on the
operations web server, it can be tested directly by requesting it using `curl`.

    curl -d "repo=acme&label=Anvils&package=anvils" \
        --get http://ops.acme.com/cgi/yum-repoquery.cgi
 
The server response should return JSON data resembling the example below:

    TODO: include JSON example
 
Now in place, jobs can request the option model data like so:

     <option name="package" enforcedvalues="true" required="true"
        valuesUrl="http://ops.acme.com/cgi/yum-repoquery.cgi?package=anvils"/> 

The Rundeck UI will display the package names in the menu and once
selected, the Job will have the matching package versions.
 
[Yum]: http://yum.baseurl.org/
[RPM]: http://www.rpm.org/
[repoquery]: http://linux.die.net/man/1/repoquery


## Summary

After reading this chapter you should understand how to run Jobs with
options, as well as, add and edit them. If you are interested in
generating option data for one of your jobs, see the
[option model provider](rundeck-by-example.html#option-model-provider-examples) section in the
[Examples](#rundeck-by-example) chapter.
