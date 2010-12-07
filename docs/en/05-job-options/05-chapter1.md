# Job Options

Any command or script can be wrapped as a Job. Creating a Job for
every use case will proliferate a large number of Jobs differing only
by how the Job jobs calls the scripts. These
differences are often environment or application version
related. Other times only the person running the Job can provide the
needed information to run the Job correctly. 

Making your scripts and commands data driven, will also make them
more generic and therefore, resuable in different contexts. Rather than
maintain variations of the same basic process, letting Jobs be driven
by a model of options from externally provided data will lead to
better abstraction and encapsulation of your process.

RunDeck Jobs can be configured to prompt a user for input by defining
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
URL opens the door to integrating RunDeck with other tools and
incorporating their data into Job workflows. 

## Prompting the user

The obvious effect from defining Job options is their appearance to
the user running the Job. Users will be presented a page called "Choose
Execution Options..." where input and menu choices must be configured.

Command line users executing Jobs via the <code>run</code> shell
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


## Options editor

Options can be created for any stored Job. The Job edit page contains
an area displaying a summary to existing options and a link to add new
ones or edit existing ones.

The option summary shows each option and its default value if it defines
them.

Clicking the "edit" link opens up the options editor. 

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

Options can also be defined as part of an XML job definition and later
loaded to the RunDeck server. See "job-v10(1)" and "rd-jobs(1)" manual
pages if you prefer using an XML Job definition.

## Defining an option

New options can be defined by pressing the "Add an option" link while
existing ones can be changed by pressing their "edit" link.

The option definition form is organized into several areas:

Identification

:    Here you provide the option's name and description. The name
     becomes part of acceptable arguments to the Job while the
     description will be provided as help text to users running the Job.

Allowed values

:    Allowed values provide a model of possible choices.
     This can contain a static list of values or a URL to a server
     providing option data. Values can be specified as a comma
     separated list as seen above but can also be requested from an
     external source using a "remote URL" [See below](#remote-option-values).
     

Restrictions

:    Defines criteria on which input to accept or present. Option
     choices can be controlled using the "Enforced from values"
     restriction. When set "true", RunDeck will only present a
     popup menu. If set "false", a text field will also be presented. 
     Enter a regular expression in the "Match Regular Expression"
     field the Job will evaluate when run.

Requirement

:    Indicates if the Job can only run if a choice is provided for
     that Option. Choosing "No" states the option is not required
     Choose "Yes" to state the option is required.

Once satisfied with the option definition, press the "Save" button to
add it to the Job definition. Pressing the "Cancel" button will
dismiss the changes and close the form.

## Remote option values

A model of option values can be retrieved from an external source.
When the `valuesUrl` is specified for an Option, then the model of
allowed values is retrieved from the specified URL. 

This is useful in a couple of scenarios when RunDeck is used to coordinate process that depend on other systems:

* Deploying packages or artifacts produced by a build or CI server, e.g. Hudson.
    * A list of recent Hudson build artifacts can be imported as Options data, so that a User can pick an appropriate package name to deploy from a list.
* Selecting from a set of available environments, defined in a CMDB
* Any situation in which input variables for your Jobs must be selected from some set of values produced by a different system.

See [Chapter 9 - Option Model Provider](#option-model-provider).

## Script usage

Option values can be passed to scripts as an argument or referenced
inside the script via a named token. Option values can be accessed in
one of several ways:

Value passed as an environment variable:
:    Bash: $RD\_OPTION\__NAME_ [^1]

Value passed as an argument to a script:
:    Commandline Arguments: ${option._name_}

Value referenced as a replacement token inside the script:
:    Script Content: @option._name_@

A single example helps illustrate these methods. Imagine a trivial script
is wrapped in a Job named "hello" and has an option named "message".
The "hello" Job option signature would be "-message <>". Here's the
content of this simple script. 

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ {.bash}
    #!/bin/sh    
    echo envvar=$RD_OPTION_MESSAGE ;# read from environment
    echo args=$1                   ;# comes from argument vector
    echo message=@option.message@  ;# replacement token
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 

When the user runs the "hello" job they will be prompted for the
"message" value. Let's assume they type the word "hello" in
response. The output of the Job will be:

    envar=hello
    args=hello    
    message=hello    

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

[^1]: To pass environment variables through remote command
dispatches, it is required to properly configure the SSH server on the
remote end. See the AcceptEnv directive in the "sshd\_config(5)"
manual page for instructions. Use a wild card pattern to permit RD\_
prefixed variables to provide open access to RunDeck generated
environment variables.


## Calling a Job with options

Jobs can be invoked from the command line using the <code>run</code>
shell tool or as a step in another Job's workflow.

Using the <code>run</code> command pass them after the double hyphen:

    run -j jobId -- -paramA valA -paramB valB

Inside an XML definition, insert them as an <code>arg</code> element:

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ {.xml}
<command>
    <jobref group="test" name="other tests">
        <arg line="-paramA valA -paramB valB"/>
    </jobref>
</command>
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 

Consult the "run(1)" and "job-v20(5)" manual pages for additional
information.
     
## Summary

After reading this chapter you should understand how to run Jobs with
options, as well as, add and edit them. If you are interested in
generating option data for one of your jobs, see the
[option model provider](#option-model-provider-examples) section in the
[Examples](#rundeck-by-example) chapter.
