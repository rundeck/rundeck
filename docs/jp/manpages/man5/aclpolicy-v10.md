% ACLPOLICY(5) Rundeck User Manuals | Version 1.4
% Noah Campbell;Greg Schueler
% August 31, 2011

# Overview

This document describes the YAML format for ACL Policy definition files.

Multiple aclpolicy files can be stored in the
same directory, helping the management of each set of rules.  This
reduces the complexity of each file.  The default path is

* RPM install: `/etc/rundeck`
* Launcher install: `$RDECK_BASE/etc`

Policy files are parsed using YAML and while the structure is rigid, 
additional information can be added and safely ignored.  So creating arbitrary 
elements for documentation or organizational purposes is gracefully parsed.  
The resulting file must be a valid yaml file.

For more information about the exact resources and actions you need to
authorize for the Rundeck application, see the 
[Rundeck User Manual - Authorization](manual/administration.html#authorization).

## Authorizing a certain action on a resource

The aclpolicy describes actions that are allowed or denied on certain resources.

The method for determining whether a user has access to perform such an action
happens essentially in this way:

1. Look for policies matching the username or rolename for the user
2. Look for a context matching the environment of the given resource
    * either a specific project by name
    * or the application level
3. Look for a Type rule for the type of resource
    * either a specific resource by its named type
    * or the generic "resource"
4. Iterate through the rules for the type
    * if a rule matches, and allows the action, mark it and continue.
    * if a rule matches and denies the action, return DENIED, and stop
5. If it was DENIED, return DENIED. If it was marked, return ALLOWED. Otherwise 
   if no rules matched, return REJECTED. 

Thus to allow an action, there has to be a matching rule that allows it, and
no matching rule that denies it.

## Changes

The YAML format has changed since version 1.2 to address several issues:

1. Acccess control on resources other than Jobs can now be declared
2. Project level access control is now supported
3. "Deny" rules can now be declared
4. Application level access control is also supported, replacing the Role mapping

The Rundeck server no longer uses role-mapping and instead defers to the aclpolicy for all authorizations.

## Upgrading

Note: The XML format from Rundeck 1.3 and earlier is no longer supported.  As 
well, the YAML format from 1.2 is now only partially supported.

If you are upgrading from Rundeck 1.3 or earlier, you will have to modify
your *.aclpolicy files. 

If you have XML formatted files, you will need to remove and replace them with
a YAML document in the format described below.  A full, admin-level ACL
is described at the end of this document.

If you have YAML formatted files, you will also need to upgrade them slightly.

## The aclpolicy markup by example

An example policy document.

    description: Yaml Policy 1
    context: # declares the context of the ACL
      project: '.*' # applies to projects matching a regex.
    for:
      resource:
         - equals:
            kind: job
           allow: '*'
      job:
         - allow: '*'
         - match:
             group: 'group1/.*'
           deny: '*'

    # note, the 1.2 format "rules" section can still be used if no
    # "for: job: " section is used
    rules:
      ^$:
        actions: 'foobar'
        
      /listAction/.*:
        actions: [action_list_1,action_list_2]

    by:
        username: 'yml_usr_1'
        group: ['yml_group_1','group2']

An .aclpolicy supports multiple policy definitions in the form of YAML 
documents usign the `---` separator.  There are four elements that make a 
policy definition: `decription`, `context`, `for`, `by`.  

It's recommended that this description be short and descriptive as it appears
in the log output.

## `context`

The `context` section declares the scope of the ensuing policy description.

The `context` can contain one of two things:

1. `project`
2. `application`

Declaring a `project:` declares the name of the project(s) for which the policy
applies.  Its value is a String, and can be a regular expression, for which
the project name must match to apply.

If you declare an `application` section, its only supported value is `rundeck`,
as:
    context:
      application: 'rundeck'

This declares that the policy document describes access control at the 
application level, rather than for at a project level.  You can then declare
access control on actions such as creating Projects.

Note that to provide a full "admin" level access control for a user or group,
then two policies must be defined, for application level as well as for project
level.

**NOTE** if you are upgrading a yaml 1.2 format document, you will need to add 
a `context` section.

## `for`

The `for` section declares a set of resource types, each containing a sequence
of matching rules which allow or deny certain actions.

Resource types declare the type of a specific resource for the match, and the generic 
"resource" is used to declare rules for all resources of a certain type.

Inside `for` is an entry for any of these resource types:

* `job` - a Rundeck Job
* `node` - a Node resource
* `adhoc` - an Ad-hoc execution
* `project` - a Project
* `resource` - indicates rules for all resources of a certain kind

Within each type section is a sequence of rules.  Recall that in YAML, a 
sequence is defined using multiple `-` indicators, or within `[` and `]` and separated by commas.

Yaml sequences:

    - a
    - b

also:

    [ a, b ]

### Type rules

Type rules are in the form:

    matching*:
     property: value
    allow: actions
    deny: actions

Each rule has one or more of these Action entries:

* `allow` - (List or String) - the actions allowed
* `deny` - (List or String) - the actions denied

It also has one or more of these "Matching" entries:

* `match` - (List or String) - regular expression matches
* `equals` - (String) - equality matches
* `contains` - (List or String) - set membership matches

Each Matching entry is composed of `property: value`, which declare what property
of the resource to test, and what value or values to apply the matching rule to.

For example, to declare a rule for a resource with a "name" property of "bob"
exactly, use `equals`:

    equals:
      name: bob
    allow: [action1, action2]
    deny: action3

For regular expression matching, use `match`:

    match:
      name: 'bob|sam'

For set membership matches, such as matching a Node that must have three 
different tags, you can use `contains`

    contains:
      tags: [a,b,c]

The `match` and `contains` allow a list of property values, and all of them
must match the resource's property for the rule to match.  This allows the basic
boolean AND logic.  For OR logic, you can simply declare another rule in the 
sequence since all rules are checked (except in the case of an explicit deny).

## `by`
    
Within `by` are `username` and `group` entries that declare who the policy applies to.

Each entry can contain a single string, or a sequence of strings to define 
multiple entries.

A single match will result in further evaluation of the policy.  

`username` elements are evaluated first as they are typically more restrictive.  
`group` elements are evaluated second.  Ordering is not important within 
this element.

Examples:

    by:
      username: 'bob'

    by:
      group: [test,qa,prod]

    by:
      username: 
        - simon
        - frank

## legacy job matching

YAML aclpolicy files created before 1.4 would have the `rules` sections.  These
were only designed to match Jobs, and so for backwards compatibilty these rules 
sections can still be used for matching only jobs.

The changes for the newer version affect the old `rules` section in these ways:

* only actual existing Jobs are matched to the rules, meaning the 'adhoc' and 
    'ui' pseudo-groups no longer exist. You must translate these actions to the
    new format.
* Action names have changed from "workflow_X" to simply "X", so you will have 
  to modify the actions.

See [Rundeck User Manual - Authorization](manual/administration.html#authorization) for more information.

### `rules` Element

The `rules` element contains a map of resource paths to `actions`.  The key in
each rule, for example:

    /path:
       actions: 'an_action'

`/path` is evaluated against the resource being evaluated.  See below for a 
complete list of paths that can have ACLs applied.  The path is evaluated
as a java regex expression.  If a match is successful, then a final check
against `actions` element is performed.

### `actions` element

The actions element can be either a single value, or a list of values.  A 
single value takes the form:

    actions: 'an_action'
    
And a list takes the form:

    actions: ['an_action1','an_action2']

Note that the single tick marks are optional according to the yaml 
specification.                           

Possible values are limitless so it requires an understanding of the
job definition you're trying to run.  The best way to understand what
the actions are is to look at the rundeck-audit.log.
This will show all the options as they're being evaluated.

## Example Admin policy

This document grants full permissions to an 'admin' role:

    description: Admin project level access control. Applies to resources within a specific project.
    context:
      project: '.*' # all projects
    for:
      resource:
        - equals:
            kind: job
          allow: [create] # allow create jobs
        - equals:
            kind: node
          allow: [read,create,update,refresh] # allow refresh node sources
        - equals:
            kind: event
          allow: [read,create] # allow read/create events
      adhoc:
        - allow: [read,run,kill] # allow running/killing adhoc jobs
      job: 
        - allow: [create,read,update,delete,run,kill] # allow create/read/write/delete/run/kill of all jobs
      node:
        - allow: [read,run] # allow read/run for nodes
    by:
      group: admin
    
    ---
    
    description: Admin Application level access control, applies to creating/deleting projects, admin of user profiles, viewing projects and reading system information.
    context:
      application: 'rundeck'
    for:
      resource:
        - equals:
            kind: project
          allow: [create] # allow create of projects
        - equals:
            kind: system
          allow: [read] # allow read of system info
        - equals:
            kind: user
          allow: [admin] # allow modify user profiles
      project:
        - match:
            name: '.*'
          allow: [read,admin] # allow view/admin of all projects
    by:
      group: admin

The Rundeck source code and all documentation may be downloaded from
<https://github.com/dtolabs/rundeck/>.
