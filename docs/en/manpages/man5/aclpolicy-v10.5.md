% ACLPOLICY(5) RunDeck User Manuals | Version 1.2
% Noah Campbell
% March 11, 2011

# Overview

Instead of using a strict xml schema, a markup language is used so new
tags can be introduced in subsequent releases while preserving
backwards compatibility.

Another benefit is that multiple aclpolicy files can be stored in the
same directory, helping the management of each set of rules.  This
reduces the complexity of each file.  The default path is

* RPM install: `/etc/rundeck`
* Launcher install: `$RDECK_BASE/etc`

Policy files are parsed using YAML and while the structure is rigid, 
additional information can be added and safely ignored.  So creating arbitrary 
elements for documentation or organizational purposes is gracefully parsed.  
The resulting file must be a valid yaml file.

In addition, existing XML formats are still supported to provide a transition.  
A warning is given only once for each file that is detected to be xml in the
log file.

## The aclpolicy markup by example

An example policy document.

    description: Yaml Policy 1
    context:
      project: 'test.*'
    rules:
      ^$:
        actions: 'foobar'
    
      /yml:
        actions: 'exact_match'
    
      /yml.*: 
        actions: pattern_match
        
      /listAction/.*:
        actions: [action_list_1,action_list_2]

    by:
        username: 'yml_usr_1'
        group: 'yml_group_1'

An .aclpolicy supports multiple policy definitions in the form of YAML 
documents usign the `---` separator.  There are three elements that make a 
policy definition: `decription`, `rules`, `by`.  

It's recommended that this description be short and descriptive as it appears
in the log output.

## `context` 

Context specifies the environmental context that the policy applies to.

You can specify a project name, regex, or "*" to apply only to the matching projects:

    context:
      project: project1

Regex:

    context:
      project: "qa.*$"

The string "*" can be used to apply to all projects:

    context:
      project: '*'

## `by` Element
    
Within a `by` can be any number of `username` and `group` elements.
A single match will result in further evaluation of the policy.  
`username` elements are evaluated first as they are typically more restrictive.  
`group` elements are evaluated second.  Ordering is not important within 
this element.

    
## `rules` Element

The `rules` element contains a map of resource paths to `actions`.  The key in
each rule, for example:

    /path:
       actions: 'an_action'

`/path` is evaluated against the resource being evaluated.  See below for a 
complete list of paths that can have ACLs applied.  The path is evaluated
as a java regex expression.  If a match is successful, then a final check
against `actions` element is performed.

## `actions` element

The actions element can be either a single value, or a list of values.  A 
single value takes the form:

    actions: 'an_action'
    
And a list takes the form:

    actions: ['an_action1','an_action2']

Note that the single tick marks are optional according to the yaml 
specification.

Known path/actions combinations are:

-------------------------------------------------------------------------------
Group     Job               Actions
--------  ----------------- ----------------------------------------------
adhoc      Temporary_Job    workflow_read

adhoc       adhoc           workflow_read, workflow_kill, workflow_read

ui          adhoc_run       workflow_run

ui          create          workflow_create, workflow_run

ui          run_and_forget  workflow_run

*           *               workflow_read, workflow_create, workflow_update, 
                            workflow_delete, workflow_kill, workflow_run, 
                            events_read, events_create, events_update, 
                            events_delete, resources_read, resources_create, 
                            resources_update, resources_delete

-------------------------------------------------------------------------------                           

Possible values are limitless so it requires an understanding of the
job definition you're trying to run.  The best way to understand what
the actions are is to look at the rundeck-audit.log.
This will show all the options as they're being evaluated.


The RunDeck source code and all documentation may be downloaded from
<https://github.com/dtolabs/rundeck/>.
