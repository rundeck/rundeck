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

## Secure Options

The built-in [SSH Provider](plugins.html#ssh-provider) for node execution allows using passwords for SSH and/or Sudo authentication mechanisms, and the passwords are supplied by Secure Options defined in a Job.

A Secure Option will always show a password prompt in the GUI, instead of a normal text field or drop down menu.

Secure Options have some limitations compared to regular options:

* The values entered by the user are not available for normal script and command option value expansion. This means that they can only be used for the purposes of the SSH Provider at the moment.
* Secure Options do not support allow Multi-valued input
* Secure option values are not stored with the Execution as are the other option values.

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

Secure Input

:   Set to true to define a Secure Option.  The multi-valued option will be disabled.

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
inside the script via a named token. Option values can be accessed in
one of several ways:

Value passed as an environment variable:
:    Bash: $RD\_OPTION\__NAME_ (**See note below**)

Value passed as an argument to a script:
:    Commandline Arguments: ${option._name_}

Value referenced as a replacement token inside the script:
:    Script Content: @option._name_@

A single example helps illustrate these methods. Imagine a trivial script
is wrapped in a Job named "hello" and has an option named "message".

The "hello" Job option signature would be: `-message <>`.

![Option usage](../figures/fig0504.png)

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
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ {.bash}         
        message=@option.message@
        if [ "$message" == "@option.message@" ] ; then
           message=mydefault
        fi 
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

**Note**: To pass environment variables through remote command
dispatches, it is required to properly configure the SSH server on the
remote end. See the AcceptEnv directive in the "sshd\_config(5)"
manual page for instructions. Use a wild card pattern to permit RD\_
prefixed variables to provide open access to Rundeck generated
environment variables.


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
2. It must be accessible via HTTP or on the local disk for the Rundeck server.
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

*Examples*

    http://server.com/test?name=${option.name}

Passes the option name as the "name" query parameter to the URL.

    http://server.com/test?jobname=${job.name}&jobgroup=${job.group}

Passes the job name and group as query parameters.

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
