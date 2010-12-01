% ACLPOLICY(5) RunDeck User Manuals | Version 1.0
% Noah Campbell
% November 30, 2010

# Overview

Instead of using a strict xml schema, a markup language is used so new
tags can be introduced in subsequent releases while preserving
backwards compatibility.

Another benefit is that multiple aclpolicy files can be stored in the
same directory, helping the management of each set of rules.  This
reduces the complexity of each file.  The default path is

* RPM install: `/etc/rundeck/client`
* Launcher install: `$RDBASE_BASE/etc`

Policy files are parsed using XPath that is fairly liberal in what it
excepts.  So creating arbitrary blocks for documentation or
organizational purposes is gracefully parsed.  The resulting file must
be a valid xml file.

## The aclpolicy markup by example

Default policy file contains this content:

    <policies>
      <policy description="Administrative group that has access to execute all jobs and modules in any project.">
        <context project="*">
          <command group="*" job="*" actions="*"/>
          <command name="*" module="*" actions="*"/>
        </context>
        <by>
          <group name="admin"/>
        </by>
      </policy>
    </policies>

The top level element of every policy file is `<policies>`.  This is a
container elements that can contain one or more `<policy>` elements.
The `<policy>` element contains a single attribute, description that
used in the audit log.  It's recommended that this description be
short and descriptive.
    
## `<policy>` Element    
    
For each `<policy` there is a `<by>` element.  The by element helps
narrow the search for policies based on the credentials of the subject
that authenticated to RunDeck.  In the example above, the `<by>`
element comes after a `<context>` element.  This is by convention
only.  The `<by>` element needs to be a child to the `<policy>`.

    
## `<by>` Element
    
Within a `<by>` can be any number of `<user>` and `<group>` elements.
These elements must all match in order for this policy to be consider
for further evaluation.  `<user>` elements are evaluated first as they
are typically more restrictive.  `<group>` elements are evaluated
second.  Order is not important within this element.

    
## `<command>` Element

The `<command>` element will match modules or jobs.  For example:
`<command group="*" job="*" actions="*"/>` says for any job in any
group with any action, grant access to the Subject in the `<by>`
clauses.  the group, attribute and action elements can use regular
expressions (java regex).  The exception is the * which is shorthand
for '^.*$'.

Known actions are:

* admin
* user_admin
* workflow_read
* workflow_create
* workflow_update
* workflow_delete
* workflow_kill
* workflow_run
* events_read
* events_create
* events_update
* events_delete
* resources_read
* resources_create
* resources_update
* resources_delete

Possible values are limitless so it requires an understanding of the
job definition you're trying to run.  The best way to understand what
the actions are is to look at the rd-audit.log.
This will show all the options as they're being evaluated.

## `<context>` Element

...is currently ignored.

